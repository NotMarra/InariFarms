package dev.notmarra.inarifarms.tasks

import dev.notmarra.inarifarms.crops.Crop
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
import dev.notmarra.inarifarms.data.CropState
import org.bukkit.Chunk
import org.bukkit.block.data.Ageable
import org.bukkit.plugin.Plugin

class CropGrowthTask(
    private val plugin: Plugin,
    private val dataManager: CropDataManager,
    private val cropRegistry: CropRegistry
) : Runnable {

    override fun run() {
        val now = System.currentTimeMillis()

        for (world in plugin.server.worlds) {
            for (chunk in world.loadedChunks) {
                val crops = dataManager.getChunkCrops(chunk)
                if (crops.isEmpty()) continue

                val pendingUpdates = mutableListOf<PendingGrowth>()
                var chunkModified = false

                for ((coords, state) in crops) {
                    if (now >= state.nextGrowthTime) {
                        val cropDef = cropRegistry.getCrop(state.cropTypeId) ?: continue
                        if (state.currentStage >= cropDef.maxGrowthStage) continue

                        state.currentStage++
                        val timerPerStage = (cropDef.growthTime * 1000L) / cropDef.maxGrowthStage
                        state.nextGrowthTime = now + timerPerStage
                        chunkModified = true

                        val (x, y, z) = coords.split(",").map { it.toInt() }
                        pendingUpdates.add(PendingGrowth(chunk, x, y, z, state, cropDef))
                    }
                }

                if (!chunkModified) continue

                val cropsCopy = HashMap(crops)

                plugin.server.regionScheduler.run(plugin, world, chunk.x, chunk.z) {
                    dataManager.saveChunkCrops(chunk, cropsCopy)
                    for (update in pendingUpdates) {
                        applyBlockUpdate(update)
                    }
                }
            }
        }
    }

    private fun applyBlockUpdate(update: PendingGrowth) {
        val block = update.chunk.getBlock(update.x, update.y, update.z)
        val blockData = block.blockData
        if (blockData is Ageable) {
            val newAge = (update.state.currentStage.toFloat() / update.cropDef.maxGrowthStage * blockData.maximumAge).toInt()
            blockData.age = minOf(newAge, blockData.maximumAge)
            block.setBlockData(blockData, false)
        }
    }
}

data class PendingGrowth(
    val chunk: Chunk,
    val x: Int, val y: Int, val z: Int,
    val state: CropState,
    val cropDef: Crop
)