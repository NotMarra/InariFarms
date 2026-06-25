package dev.notmarra.inarifarms.display

import dev.notmarra.inarifarms.Inarifarms
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class HoverDisplayManager(
    private val plugin: Inarifarms,
    private val dataManager: CropDataManager,
    private val cropRegistry: CropRegistry
) : Listener {

    private val playerDisplays = HashMap<UUID, TextDisplay>()
    private val conf = plugin.configManager.config

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            for (player in Bukkit.getOnlinePlayers()) {
                updateHoverDisplay(player)
            }
        }, 0L, 3L)

        for (player in Bukkit.getOnlinePlayers()) {
            spawnDisplayFor(player)
        }
    }

    fun cleanup() {
        playerDisplays.values.forEach { it.remove() }
        playerDisplays.clear()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        spawnDisplayFor(event.player)
    }

    private fun spawnDisplayFor(player: Player) {
        playerDisplays[player.uniqueId]?.remove()
        val spawnLoc = player.location.clone()

        val display = player.world.spawn(spawnLoc, TextDisplay::class.java) { textDisplay ->
            textDisplay.billboard = Display.Billboard.CENTER
            textDisplay.backgroundColor = org.bukkit.Color.fromARGB(120, 15, 15, 15)
            textDisplay.isPersistent = false
            textDisplay.isShadowed = true
        }

        for (onlinePlayer in Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.hideEntity(plugin, display)
            }
        }
        playerDisplays[player.uniqueId] = display
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerDisplays.remove(event.player.uniqueId)?.remove()
    }

    private fun updateHoverDisplay(player: Player) {
        val display = playerDisplays[player.uniqueId] ?: return
        val targetBlock = player.getTargetBlockExact(5)

        if (targetBlock != null && !targetBlock.type.isAir) {
            val cropState = dataManager.getCropData(targetBlock)

            if (cropState != null) {
                val displayLoc = targetBlock.location.add(0.5, 1.2, 0.5)
                val crop = cropRegistry.getCrop(cropState.cropTypeId)
                val cropName = crop?.displayName ?: "NaN"
                val mm = MiniMessage.miniMessage()

                val textLines = conf.displayText.map {
                    it.replace("%displayName%", cropName)
                        .replace("%moisture%", cropState.currentMoisture.toString())
                        .replace("%stage%", cropState.currentStage.toString())
                        .replace("%finalStage%", crop?.maxGrowthStage.toString())
                }

                val components = textLines.map { mm.deserialize(it) }

                val finalText = components.reduce { acc, component ->
                    acc.append(Component.newline()).append(component)
                }

                display.teleport(displayLoc)
                display.text(finalText)
                return
            }
        }

        val hideLoc = player.location.clone().apply { y = y - 100 }
        if (display.location.distance(hideLoc) > 1.0) {
            display.teleport(hideLoc)
        }
    }
}