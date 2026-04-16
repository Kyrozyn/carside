package me.kyrozyn.carside.data.offline

import me.kyrozyn.carside.domain.model.CityPreset
import me.kyrozyn.carside.domain.model.OfflineMapBounds

/**
 * Preset kota/wilayah Indonesia (pusat ± span derajat).
 * Diurutkan provinsi lalu nama untuk daftar yang konsisten.
 */
private fun p(
    id: String,
    name: String,
    province: String,
    lat: Double,
    lon: Double,
    span: Double = 0.16,
): CityPreset = CityPreset(
    id = id,
    displayName = name,
    province = province,
    bounds = OfflineMapBounds(
        north = lat + span,
        south = lat - span,
        east = lon + span,
        west = lon - span,
    ),
)

private val ALL_PRESETS: List<CityPreset> = listOf(
    // Aceh
    p("aceh_banda_aceh", "Banda Aceh", "Aceh", 5.55, 95.32),
    p("aceh_lhokseumawe", "Lhokseumawe", "Aceh", 5.18, 97.15),
    p("aceh_langsa", "Langsa", "Aceh", 4.48, 97.97),
    p("aceh_meulaboh", "Meulaboh", "Aceh", 4.15, 96.13),
    // Sumatera Utara
    p("sumut_medan", "Medan", "Sumatera Utara", 3.60, 98.68),
    p("sumut_pematang_siantar", "Pematang Siantar", "Sumatera Utara", 2.96, 99.07),
    p("sumut_binjai", "Binjai", "Sumatera Utara", 3.60, 98.49),
    p("sumut_padang_sidempuan", "Padang Sidempuan", "Sumatera Utara", 1.38, 99.27),
    p("sumut_sibolga", "Sibolga", "Sumatera Utara", 1.74, 98.78),
    // Sumatera Barat
    p("sumbar_padang", "Padang", "Sumatera Barat", -0.95, 100.36),
    p("sumbar_bukittinggi", "Bukittinggi", "Sumatera Barat", -0.30, 100.37),
    p("sumbar_padang_panjang", "Padang Panjang", "Sumatera Barat", -0.47, 100.41),
    // Riau
    p("riau_pekanbaru", "Pekanbaru", "Riau", 0.51, 101.45),
    p("riau_dumai", "Dumai", "Riau", 1.67, 101.45),
    // Kepulauan Riau
    p("kepri_batam", "Batam", "Kepulauan Riau", 1.13, 104.03),
    p("kepri_tanjung_pinang", "Tanjung Pinang", "Kepulauan Riau", 1.05, 104.49),
    // Jambi
    p("jambi_kota", "Jambi", "Jambi", -1.61, 103.61),
    // Sumatera Selatan
    p("sumsel_palembang", "Palembang", "Sumatera Selatan", -2.98, 104.76),
    p("sumsel_prabumulih", "Prabumulih", "Sumatera Selatan", -3.43, 104.24),
    p("sumsel_lubuklinggau", "Lubuklinggau", "Sumatera Selatan", -3.29, 102.86),
    // Bengkulu
    p("bengkulu_kota", "Bengkulu", "Bengkulu", -3.80, 102.27),
    // Lampung
    p("lampung_bandar", "Bandar Lampung", "Lampung", -5.43, 105.27),
    p("lampung_metro", "Metro", "Lampung", -5.11, 105.31),
    // DKI Jakarta
    p("dki_jakarta", "Jakarta Pusat", "DKI Jakarta", -6.18, 106.83),
    p("dki_jakarta_selatan", "Jakarta Selatan", "DKI Jakarta", -6.26, 106.81),
    // Banten
    p("banten_serang", "Serang", "Banten", -6.12, 106.15),
    p("banten_tangerang", "Tangerang", "Banten", -6.18, 106.63),
    p("banten_cilegon", "Cilegon", "Banten", -6.01, 106.06),
    // Jawa Barat
    p("jabar_bandung", "Bandung", "Jawa Barat", -6.92, 107.61),
    p("jabar_bogor", "Bogor", "Jawa Barat", -6.60, 106.80),
    p("jabar_depok", "Depok", "Jawa Barat", -6.40, 106.82),
    p("jabar_bekasi", "Bekasi", "Jawa Barat", -6.24, 107.00),
    p("jabar_cirebon", "Cirebon", "Jawa Barat", -6.73, 108.55),
    p("jabar_tasikmalaya", "Tasikmalaya", "Jawa Barat", -7.33, 108.22),
    p("jabar_sukabumi", "Sukabumi", "Jawa Barat", -6.93, 106.93),
    p("jabar_karawang", "Karawang", "Jawa Barat", -6.32, 107.34),
    // Jawa Tengah
    p("jateng_semarang", "Semarang", "Jawa Tengah", -6.99, 110.42),
    p("jateng_solo", "Solo (Surakarta)", "Jawa Tengah", -7.57, 110.83),
    p("jateng_purwokerto", "Purwokerto", "Jawa Tengah", -7.43, 109.25),
    p("jateng_pekalongan", "Pekalongan", "Jawa Tengah", -6.89, 109.67),
    p("jateng_tegal", "Tegal", "Jawa Tengah", -6.87, 109.12),
    p("jateng_kudus", "Kudus", "Jawa Tengah", -6.80, 110.84),
    p("jateng_magelang", "Magelang", "Jawa Tengah", -7.48, 110.22),
    p("jateng_salatiga", "Salatiga", "Jawa Tengah", -7.33, 110.50),
    // DI Yogyakarta
    p("diy_yogyakarta", "Yogyakarta", "DI Yogyakarta", -7.80, 110.37),
    // Jawa Timur
    p("jatim_surabaya", "Surabaya", "Jawa Timur", -7.26, 112.75),
    p("jatim_malang", "Malang", "Jawa Timur", -7.98, 112.63),
    p("jatim_kediri", "Kediri", "Jawa Timur", -7.82, 112.02),
    p("jatim_blitar", "Blitar", "Jawa Timur", -8.10, 112.17),
    p("jatim_pasuruan", "Pasuruan", "Jawa Timur", -7.65, 112.91),
    p("jatim_probolinggo", "Probolinggo", "Jawa Timur", -7.76, 113.22),
    p("jatim_jember", "Jember", "Jawa Timur", -8.17, 113.70),
    p("jatim_banyuwangi", "Banyuwangi", "Jawa Timur", -8.22, 114.37),
    p("jatim_madiun", "Madiun", "Jawa Timur", -7.63, 111.52),
    // Bali
    p("bali_denpasar", "Denpasar", "Bali", -8.65, 115.22),
    p("bali_singaraja", "Singaraja", "Bali", -8.11, 115.09),
    // Nusa Tenggara Barat
    p("ntb_mataram", "Mataram", "Nusa Tenggara Barat", -8.58, 116.10),
    p("ntb_bima", "Bima", "Nusa Tenggara Barat", -8.46, 118.75),
    // Nusa Tenggara Timur
    p("ntt_kupang", "Kupang", "Nusa Tenggara Timur", -10.17, 123.61),
    p("ntt_ende", "Ende", "Nusa Tenggara Timur", -8.84, 121.66),
    // Kalimantan Barat
    p("kalbar_pontianak", "Pontianak", "Kalimantan Barat", -0.03, 109.34),
    p("kalbar_singkawang", "Singkawang", "Kalimantan Barat", 0.91, 109.00),
    // Kalimantan Tengah
    p("kalteng_palangkaraya", "Palangkaraya", "Kalimantan Tengah", -2.21, 113.92),
    p("kalteng_sampit", "Sampit", "Kalimantan Tengah", -2.53, 112.95),
    // Kalimantan Selatan
    p("kalsel_banjarmasin", "Banjarmasin", "Kalimantan Selatan", -3.32, 114.59),
    p("kalsel_banjarbaru", "Banjarbaru", "Kalimantan Selatan", -3.44, 114.83),
    // Kalimantan Timur
    p("kaltim_samarinda", "Samarinda", "Kalimantan Timur", -0.50, 117.15),
    p("kaltim_balikpapan", "Balikpapan", "Kalimantan Timur", -1.24, 116.86),
    p("kaltim_bontang", "Bontang", "Kalimantan Timur", 0.13, 117.50),
    p("kaltim_tarakan", "Tarakan", "Kalimantan Timur", 3.30, 117.59),
    // Kalimantan Utara
    p("kalut_tanjung_selor", "Tanjung Selor", "Kalimantan Utara", 2.85, 117.37),
    // Sulawesi Utara
    p("sulut_manado", "Manado", "Sulawesi Utara", 1.47, 124.85),
    p("sulut_bitung", "Bitung", "Sulawesi Utara", 1.44, 125.20),
    // Gorontalo
    p("gorontalo_kota", "Gorontalo", "Gorontalo", 0.54, 123.06),
    // Sulawesi Tengah
    p("sulteng_palu", "Palu", "Sulawesi Tengah", -0.90, 119.87),
    p("sulteng_poso", "Poso", "Sulawesi Tengah", -1.39, 120.76),
    // Sulawesi Selatan
    p("sulsel_makassar", "Makassar", "Sulawesi Selatan", -5.15, 119.42),
    p("sulsel_parepare", "Parepare", "Sulawesi Selatan", -4.01, 119.62),
    p("sulsel_palopo", "Palopo", "Sulawesi Selatan", -3.00, 120.20),
    // Sulawesi Tenggara
    p("sultra_kendari", "Kendari", "Sulawesi Tenggara", -3.97, 122.51),
    p("sultra_bau_bau", "Bau-Bau", "Sulawesi Tenggara", -5.47, 122.60),
    // Sulawesi Barat
    p("sulbar_mamuju", "Mamuju", "Sulawesi Barat", -2.68, 118.89),
    // Maluku
    p("maluku_ambon", "Ambon", "Maluku", -3.70, 128.18),
    p("maluku_tual", "Tual", "Maluku", -5.63, 132.75),
    // Maluku Utara
    p("malut_ternate", "Ternate", "Maluku Utara", 0.79, 127.39),
    p("malut_tobelo", "Tobelo", "Maluku Utara", 1.73, 128.01),
    // Papua Barat
    p("pabar_sorong", "Sorong", "Papua Barat", -0.88, 131.25),
    p("pabar_manokwari", "Manokwari", "Papua Barat", -0.87, 134.08),
    p("pabar_fakfak", "Fakfak", "Papua Barat", -2.93, 132.30),
    // Papua
    p("papua_jayapura", "Jayapura", "Papua", -2.53, 140.72),
    p("papua_merauke", "Merauke", "Papua", -8.50, 140.40),
    p("papua_timika", "Timika", "Papua", -4.54, 136.89),
    // Jabodetabek agregat (lebih luas)
    p("jabodetabek", "Jabodetabek (agregat)", "Jabodetabek", -6.30, 106.85, span = 0.45),
)

val INDONESIAN_CITY_PRESETS: List<CityPreset> = ALL_PRESETS.sortedWith(
    compareBy({ it.province }, { it.displayName }),
)
