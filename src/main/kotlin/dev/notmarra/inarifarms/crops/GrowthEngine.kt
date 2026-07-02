package dev.notmarra.inarifarms.crops

import dev.notmarra.inarifarms.data.CropState

/**
 * Pure growth logic - no I/O, no scheduler.
 * Called "pull" style, e.g. when a player opens a station's GUI. Uses the
 * stored `nextGrowthTime` to figure out how much real time passed since the
 * last check and simulates everything that would have happened in between -
 * including multiple full grow -> auto-harvest -> auto-replant cycles if the
 * seed slot has a stack of seeds (AFK farm behavior).
 *
 * The loop is naturally bounded (no risk of hanging even after a long
 * offline period): each iteration either advances one growth stage or
 * consumes one seed, and both seed count and free storage space are small,
 * finite numbers - so simulating a year of offline time is just as fast as
 * simulating a minute.
 */
object GrowthEngine {

    /**
     * Result of [advance].
     *
     * @param cropState state to persist next; null means the seed slot is now
     *   empty (last seed in the stack got consumed) and there's nothing left
     *   to grow until the player adds more seeds
     * @param remainingSeedCount seeds left in the seed slot stack (0 if empty)
     * @param harvestedCount how many mature crops were produced and should be
     *   inserted into the station's storage slots by the caller
     */
    data class AdvanceResult(
        val cropState: CropState?,
        val remainingSeedCount: Int,
        val harvestedCount: Int,
        val remainingStationWater: Int
    )

    /**
     * Advances growth from `state.nextGrowthTime` up to `now`, auto-harvesting
     * and auto-replanting as many times as the seed stack and free storage
     * space allow.
     *
     * @param state current stored crop state
     * @param crop crop definition loaded from yml (CropRegistry)
     * @param speedMultiplier growth speed multiplier (e.g. from station level)
     * @param seedCount how many seeds are currently in the seed slot stack
     *   (including the one currently growing)
     * @param freeStorageCapacity how many more harvested items can currently
     *   fit into the station's storage (empty slot capacity + free space in
     *   partially filled stacks). Pass Int.MAX_VALUE to effectively disable
     *   this limit.
     * @param now current time in ms (parameter for testability)
     */
    fun advance(
        state: CropState,
        crop: Crop,
        speedMultiplier: Double,
        seedCount: Int,
        freeStorageCapacity: Int,
        stationWater: Int,
        now: Long = System.currentTimeMillis()
    ): AdvanceResult {
        if (seedCount <= 0) return AdvanceResult(null, 0, 0, stationWater)

        val step = stageMillis(crop, speedMultiplier)

        var stage = state.currentStage
        var next = if (state.nextGrowthTime <= 0L) now + step else state.nextGrowthTime
        var seeds = seedCount
        var harvested = 0
        var currentWater = stationWater

        while (next <= now) {
            if (stage < crop.maxGrowthStage) {
                // still growing towards maturity
                if (currentWater < crop.waterConsumption) {
                    // out of water - growth stalls until watered, stop simulating further
                    next = now + step
                    break
                }
                currentWater -= crop.waterConsumption
                stage++
                next += step
                continue
            }

            // mature - auto-harvest, then either replant or stop
            if (harvested >= freeStorageCapacity) {
                // storage is full, nothing more we can do right now
                break
            }

            seeds--
            harvested++

            if (seeds <= 0) {
                // that was the last seed in the stack - nothing left to replant
                return AdvanceResult(cropState = null, remainingSeedCount = 0, harvestedCount = harvested, remainingStationWater = currentWater)
            }

            // replant: reset stage, keep accumulated moisture, keep simulating
            stage = 0
            next += step
        }

        val finalState = CropState(
            cropTypeId = state.cropTypeId,
            currentStage = stage,
            nextGrowthTime = next
        )
        return AdvanceResult(finalState, seeds, harvested, currentWater)
    }


    /**
     * High-level status of a crop, useful for GUI display (lore text, icons, ...).
     * - [GROWING]: actively progressing towards the next stage.
     * - [WAITING_WATER]: stalled because `currentMoisture < crop.waterConsumption`.
     * - [WAITING_STORAGE]: fully grown but couldn't auto-harvest last time
     *   because the station's storage slots were full.
     */
    enum class GrowthStatus { GROWING, WAITING_WATER, WAITING_STORAGE }

    fun growthStatus(state: CropState, crop: Crop, stationWater: Int): GrowthStatus = when {
        state.currentStage >= crop.maxGrowthStage -> GrowthStatus.WAITING_STORAGE
        stationWater < crop.waterConsumption -> GrowthStatus.WAITING_WATER
        else -> GrowthStatus.GROWING
    }

    /**
     * Milliseconds remaining until the next growth stage, for [GrowthStatus.GROWING]
     * crops. Returns 0 if already due (e.g. right after [advance] was just called).
     */
    fun remainingMillis(state: CropState, now: Long = System.currentTimeMillis()): Long =
        (state.nextGrowthTime - now).coerceAtLeast(0L)

    /** Duration of a single growth stage in ms, adjusted by the station's speed multiplier. */
    fun stageMillis(crop: Crop, speedMultiplier: Double): Long {
        val effectiveMultiplier = if (speedMultiplier <= 0.0) 1.0 else speedMultiplier
        return (crop.growthTime * 1000.0 / crop.maxGrowthStage / effectiveMultiplier).toLong().coerceAtLeast(1L)
    }
}