package dev.notmarra.inarifarms.listeners

import dev.notmarra.inarifarms.data.BlockDataManager
import dev.notmarra.inarifarms.stations.StationRegistry
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class StationInteractListener(
    private val blockDataManager: BlockDataManager,
    private val stationRegistry: StationRegistry
) : Listener {

    @EventHandler
    fun onStationWaterFill(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val player = event.player
        val item = event.item ?: return

        if (item.type == Material.WATER_BUCKET && blockDataManager.isStation(block)) {

            val typeId = blockDataManager.getStationType(block) ?: return
            val station = stationRegistry.getStation(typeId) ?: return
            val levelIndex = blockDataManager.getStationLevel(block)
            val stationLevel = station.getLevel(levelIndex) ?: return

            val currentWater = blockDataManager.getStationWater(block)
            val maxWater = stationLevel.maxWater

            if (currentWater < maxWater) {
                event.isCancelled = true

                val amountToAdd = 10
                val newWater = (currentWater + amountToAdd).coerceAtMost(maxWater)

                blockDataManager.setStationWater(block, newWater)

                if (player.gameMode != GameMode.CREATIVE) {
                    item.type = Material.BUCKET
                }

                player.playSound(block.location, Sound.ITEM_BUCKET_EMPTY, 1f, 1f)
            }
        }
    }
}