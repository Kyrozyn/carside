package me.kyrozyn.carside.data.fuel

import me.kyrozyn.carside.domain.model.FuelEconomySummary
import me.kyrozyn.carside.domain.model.FuelEntry

/**
 * Full-to-full: jarak = odo akhir − odo awal; bahan bakar = Σ liter isian setelah awal sampai isi penuh akhir (inklusif).
 */
internal fun computeFuelEconomySummary(entries: List<FuelEntry>): FuelEconomySummary {
    if (entries.isEmpty()) {
        return FuelEconomySummary(
            latestKmPerLiter = null,
            rollingAvgKmPerLiter = null,
            totalSpentIdr = 0L,
            totalLiters = 0.0,
            totalDistanceKm = null,
            pricePerKmIdr = null,
        )
    }

    val sorted = entries.sortedWith(
        compareBy<FuelEntry> { it.filledAtEpochMs }.thenBy { it.id },
    )

    val totalSpent = sorted.sumOf { it.totalCostIdr }
    val totalLiters = sorted.sumOf { it.liters }

    val firstOdo = sorted.first().odometerKm
    val lastOdo = sorted.last().odometerKm
    val totalDistanceKm = (lastOdo - firstOdo).takeIf { it > 0.0 }

    val pricePerKm = totalDistanceKm?.let { dist ->
        if (dist > 0.0) totalSpent.toDouble() / dist else null
    }

    val segmentKmPerLiter = mutableListOf<Double>()
    var i = 0
    while (i < sorted.size) {
        if (!sorted[i].isFullTank) {
            i++
            continue
        }
        var j = i + 1
        while (j < sorted.size && !sorted[j].isFullTank) j++
        if (j < sorted.size && sorted[j].isFullTank) {
            val odoA = sorted[i].odometerKm
            val odoB = sorted[j].odometerKm
            val distanceKm = odoB - odoA
            var fuelLiters = 0.0
            for (k in (i + 1)..j) {
                fuelLiters += sorted[k].liters
            }
            if (distanceKm > 0.0 && fuelLiters > 0.0) {
                segmentKmPerLiter += distanceKm / fuelLiters
            }
            i = j
        } else {
            i++
        }
    }

    val latest = segmentKmPerLiter.lastOrNull()
    val rolling = segmentKmPerLiter.takeIf { it.isNotEmpty() }?.average()

    return FuelEconomySummary(
        latestKmPerLiter = latest,
        rollingAvgKmPerLiter = rolling,
        totalSpentIdr = totalSpent,
        totalLiters = totalLiters,
        totalDistanceKm = totalDistanceKm,
        pricePerKmIdr = pricePerKm,
    )
}
