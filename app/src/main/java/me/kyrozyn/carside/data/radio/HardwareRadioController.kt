package me.kyrozyn.carside.data.radio

import android.content.Context
import android.hardware.radio.ProgramSelector
import android.hardware.radio.RadioManager
import android.hardware.radio.RadioMetadata
import android.hardware.radio.RadioTuner
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.kyrozyn.carside.data.settings.AppSettingsRepository
import me.kyrozyn.carside.domain.model.RadioBand
import me.kyrozyn.carside.domain.model.RadioPreset
import me.kyrozyn.carside.domain.model.RadioProgramInfo
import me.kyrozyn.carside.domain.model.RadioState
import me.kyrozyn.carside.domain.repository.RadioController

/**
 * Implementasi [RadioController] yang menggunakan `android.hardware.radio.RadioManager`.
 * Hanya berfungsi pada device dengan BroadcastRadio HAL (umumnya Android Automotive OS)
 * dan APK diinstal sebagai system/privileged app agar permission `ACCESS_BROADCAST_RADIO`
 * di-grant. Jika tidak tersedia, state akan bertahan di `available = false` dan semua
 * aksi tidak berefek.
 */
class HardwareRadioController(
    context: Context,
    private val settings: AppSettingsRepository,
) : RadioController {

    private val appContext = context.applicationContext
    private val radioManager: RadioManager? = runCatching {
        // Context.RADIO_SERVICE adalah @SystemApi, gunakan literalnya ("radio").
        appContext.getSystemService("radio") as? RadioManager
    }.getOrNull()

    private val handlerThread = HandlerThread("CarsideRadioHandler").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var presetsJob: Job? = null

    private var tuner: RadioTuner? = null
    private var moduleId: Int = -1
    private var fmDescriptor: RadioManager.FmBandDescriptor? = null
    private var amDescriptor: RadioManager.AmBandDescriptor? = null

    private val _state = MutableStateFlow(
        RadioState.unavailable(presets = settings.radioPresets.value),
    )
    override val state: StateFlow<RadioState> = _state.asStateFlow()

    init {
        presetsJob = scope.launch {
            settings.radioPresets.collect { list ->
                _state.update { it.copy(presets = list) }
            }
        }
    }

    override fun open() {
        val rm = radioManager
        if (rm == null) {
            _state.update {
                it.copy(available = false, errorMessage = "RadioManager tidak tersedia")
            }
            return
        }

        val modules = mutableListOf<RadioManager.ModuleProperties>()
        val status = try {
            rm.listModules(modules)
        } catch (e: SecurityException) {
            _state.update {
                it.copy(
                    available = false,
                    errorMessage = "Permission ACCESS_BROADCAST_RADIO ditolak",
                )
            }
            return
        } catch (e: Exception) {
            _state.update {
                it.copy(available = false, errorMessage = "listModules gagal: ${e.message}")
            }
            return
        }

        if (status != RadioManager.STATUS_OK || modules.isEmpty()) {
            _state.update {
                it.copy(available = false, errorMessage = "Tuner radio tidak ditemukan")
            }
            return
        }

        val module = modules.firstOrNull { m ->
            m.bands.any { bandSupportsFm(it.type) }
        } ?: run {
            _state.update {
                it.copy(available = false, errorMessage = "Modul FM tidak tersedia")
            }
            return
        }

        moduleId = module.id
        fmDescriptor = module.bands.firstOrNull { bandSupportsFm(it.type) }
            as? RadioManager.FmBandDescriptor
        amDescriptor = module.bands.firstOrNull { bandSupportsAm(it.type) }
            as? RadioManager.AmBandDescriptor

        val fm = fmDescriptor
        if (fm == null) {
            _state.update {
                it.copy(available = false, errorMessage = "Deskriptor FM kosong")
            }
            return
        }

        val fmConfig = RadioManager.FmBandConfig.Builder(fm)
            .setStereo(true)
            .setRds(true)
            .build()

        try {
            val t = rm.openTuner(module.id, fmConfig, /* withAudio = */ true, tunerCallback, handler)
            if (t == null) {
                _state.update {
                    it.copy(available = false, errorMessage = "openTuner mengembalikan null")
                }
                return
            }
            tuner = t
            val fmRange = fm.lowerLimit..fm.upperLimit
            val amRange = amDescriptor?.let { it.lowerLimit..it.upperLimit }
                ?: RadioState.DEFAULT_AM_RANGE_KHZ
            _state.update {
                it.copy(
                    available = true,
                    band = RadioBand.FM,
                    fmRangeKhz = fmRange,
                    fmStepKhz = fm.spacing.takeIf { s -> s > 0 }
                        ?: RadioState.DEFAULT_FM_STEP_KHZ,
                    amRangeKhz = amRange,
                    amStepKhz = amDescriptor?.spacing?.takeIf { s -> s > 0 }
                        ?: RadioState.DEFAULT_AM_STEP_KHZ,
                    frequencyKhz = it.frequencyKhz.takeIf { f -> f in fmRange } ?: fmRange.first,
                    errorMessage = null,
                    isMuted = false,
                )
            }
        } catch (e: SecurityException) {
            _state.update {
                it.copy(
                    available = false,
                    errorMessage = "Permission ACCESS_BROADCAST_RADIO ditolak",
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(available = false, errorMessage = "Gagal membuka tuner: ${e.message}")
            }
        }
    }

    override fun close() {
        handler.post {
            runCatching { tuner?.close() }
            tuner = null
        }
        _state.update { it.copy(isTuning = false, isScanning = false) }
    }

    override fun setBand(band: RadioBand) {
        if (!_state.value.available || tuner == null) return
        val cfg: RadioManager.BandConfig? = when (band) {
            RadioBand.FM -> fmDescriptor?.let {
                RadioManager.FmBandConfig.Builder(it).setStereo(true).setRds(true).build()
            }
            RadioBand.AM -> amDescriptor?.let {
                RadioManager.AmBandConfig.Builder(it).build()
            }
        }
        if (cfg == null) {
            _state.update { it.copy(errorMessage = "Band ${band.name} tidak didukung modul ini") }
            return
        }
        handler.post {
            runCatching { tuner?.setConfiguration(cfg) }
        }
        _state.update {
            val range = if (band == RadioBand.FM) it.fmRangeKhz else it.amRangeKhz
            it.copy(
                band = band,
                frequencyKhz = it.frequencyKhz.takeIf { f -> f in range } ?: range.first,
                program = null,
            )
        }
    }

    override fun tune(frequencyKhz: Int) {
        if (!_state.value.available || tuner == null) return
        val range = _state.value.currentRangeKhz
        val target = frequencyKhz.coerceIn(range.first, range.last)
        _state.update { it.copy(isTuning = true, errorMessage = null) }
        handler.post {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val selectorBand = when (_state.value.band) {
                        RadioBand.FM -> RadioManager.BAND_FM
                        RadioBand.AM -> RadioManager.BAND_AM
                    }
                    val selector = ProgramSelector.createAmFmSelector(selectorBand, target)
                    tuner?.tune(selector)
                } else {
                    @Suppress("DEPRECATION")
                    tuner?.tune(target, 0)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(isTuning = false, errorMessage = "Tune gagal: ${e.message}")
                }
            }
        }
    }

    override fun step(up: Boolean) {
        if (!_state.value.available || tuner == null) return
        _state.update { it.copy(isTuning = true, errorMessage = null) }
        handler.post {
            runCatching {
                tuner?.step(
                    if (up) RadioTuner.DIRECTION_UP else RadioTuner.DIRECTION_DOWN,
                    /* skipSubChannel = */ true,
                )
            }.onFailure { e ->
                _state.update {
                    it.copy(isTuning = false, errorMessage = "Step gagal: ${e.message}")
                }
            }
        }
    }

    override fun scan(up: Boolean) {
        if (!_state.value.available || tuner == null) return
        _state.update { it.copy(isScanning = true, errorMessage = null) }
        handler.post {
            runCatching {
                @Suppress("DEPRECATION")
                tuner?.scan(
                    if (up) RadioTuner.DIRECTION_UP else RadioTuner.DIRECTION_DOWN,
                    /* skipSubChannel = */ true,
                )
            }.onFailure { e ->
                _state.update {
                    it.copy(isScanning = false, errorMessage = "Scan gagal: ${e.message}")
                }
            }
        }
    }

    override fun cancelScan() {
        if (!_state.value.available || tuner == null) return
        handler.post { runCatching { tuner?.cancel() } }
        _state.update { it.copy(isScanning = false, isTuning = false) }
    }

    override fun setMuted(muted: Boolean) {
        if (!_state.value.available || tuner == null) return
        handler.post {
            runCatching { tuner?.setMute(muted) }
        }
        _state.update { it.copy(isMuted = muted) }
    }

    override fun savePreset(slot: Int) {
        val current = _state.value
        if (!current.available) return
        if (slot !in 0 until RadioState.PRESET_SLOT_COUNT) return
        val preset = RadioPreset(
            slot = slot,
            frequencyKhz = current.frequencyKhz,
            band = current.band,
            label = current.program?.stationName,
        )
        settings.setRadioPreset(preset)
    }

    override fun loadPreset(slot: Int) {
        val preset = _state.value.presets.firstOrNull { it.slot == slot } ?: return
        if (preset.band != _state.value.band) setBand(preset.band)
        tune(preset.frequencyKhz)
    }

    override fun clearPreset(slot: Int) {
        settings.clearRadioPreset(slot)
    }

    private val tunerCallback = object : RadioTuner.Callback() {
        override fun onProgramInfoChanged(info: RadioManager.ProgramInfo?) {
            handleProgramInfo(info)
        }

        override fun onMetadataChanged(metadata: RadioMetadata?) {
            if (metadata == null) return
            val freq = _state.value.frequencyKhz
            _state.update {
                it.copy(
                    isTuning = false,
                    program = buildProgramInfoFromMetadata(metadata, freq, it.band),
                )
            }
        }

        override fun onConfigurationChanged(config: RadioManager.BandConfig?) {
            if (config == null) return
            val mappedBand = mapBand(config.type)
            _state.update { st ->
                if (mappedBand != null && mappedBand != st.band) {
                    st.copy(band = mappedBand)
                } else {
                    st
                }
            }
        }

        override fun onError(status: Int) {
            _state.update {
                it.copy(
                    isTuning = false,
                    isScanning = false,
                    errorMessage = "Radio error: $status",
                )
            }
        }

        override fun onTuneFailed(result: Int, selector: ProgramSelector?) {
            _state.update {
                it.copy(isTuning = false, errorMessage = "Tune gagal (code $result)")
            }
        }
    }

    private fun handleProgramInfo(info: RadioManager.ProgramInfo?) {
        if (info == null) return
        val freq = extractFrequencyKhz(info) ?: return
        val band = bandForFrequency(freq) ?: _state.value.band
        val meta = info.metadata
        val signal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { info.signalStrength }.getOrNull()
        } else {
            null
        }

        val programInfo = if (meta != null) {
            buildProgramInfoFromMetadata(meta, freq, band).copy(signalStrength = signal)
        } else {
            RadioProgramInfo(
                frequencyKhz = freq,
                band = band,
                signalStrength = signal,
            )
        }

        _state.update {
            it.copy(
                band = band,
                frequencyKhz = freq,
                isTuning = false,
                isScanning = false,
                program = programInfo,
                errorMessage = null,
            )
        }
    }

    private fun extractFrequencyKhz(info: RadioManager.ProgramInfo): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val selector = runCatching { info.selector }.getOrNull()
            if (selector != null) {
                val primary = runCatching { selector.primaryId }.getOrNull()
                val value = primary?.value
                if (value != null && value > 0L) return value.toInt()
            }
        }
        @Suppress("DEPRECATION")
        val ch = runCatching { info.channel }.getOrNull()
        return ch?.takeIf { it > 0 }
    }

    private fun buildProgramInfoFromMetadata(
        metadata: RadioMetadata,
        frequencyKhz: Int,
        band: RadioBand,
    ): RadioProgramInfo {
        val ps = metadata.getString(RadioMetadata.METADATA_KEY_RDS_PS)
        val rt = metadata.getString(RadioMetadata.METADATA_KEY_RDS_RT)
        val title = metadata.getString(RadioMetadata.METADATA_KEY_TITLE)
        val artist = metadata.getString(RadioMetadata.METADATA_KEY_ARTIST)
        val ptyString = metadata.getString(RadioMetadata.METADATA_KEY_RDS_PTY)
            ?: metadata.getString(RadioMetadata.METADATA_KEY_RBDS_PTY)
        val ptyInt = runCatching { metadata.getInt(RadioMetadata.METADATA_KEY_RDS_PTY) }
            .getOrNull()
            ?: runCatching { metadata.getInt(RadioMetadata.METADATA_KEY_RBDS_PTY) }.getOrNull()

        val stationName = listOfNotNull(ps, title)
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
        val radioText = rt?.trim()?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(artist, title)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString(" - ")
                .takeIf { it.isNotBlank() }
        val pty = ptyString?.trim()?.takeIf { it.isNotBlank() }
            ?: ptyInt?.takeIf { it > 0 }?.toString()

        return RadioProgramInfo(
            frequencyKhz = frequencyKhz,
            band = band,
            stationName = stationName,
            radioText = radioText,
            programType = pty,
        )
    }

    private fun bandForFrequency(frequencyKhz: Int): RadioBand? {
        val st = _state.value
        return when (frequencyKhz) {
            in st.fmRangeKhz -> RadioBand.FM
            in st.amRangeKhz -> RadioBand.AM
            else -> null
        }
    }

    private fun bandSupportsFm(type: Int): Boolean =
        type == RadioManager.BAND_FM || type == RadioManager.BAND_FM_HD

    private fun bandSupportsAm(type: Int): Boolean =
        type == RadioManager.BAND_AM || type == RadioManager.BAND_AM_HD

    private fun mapBand(type: Int): RadioBand? = when {
        bandSupportsFm(type) -> RadioBand.FM
        bandSupportsAm(type) -> RadioBand.AM
        else -> null
    }
}
