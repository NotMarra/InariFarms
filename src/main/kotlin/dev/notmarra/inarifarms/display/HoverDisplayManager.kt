package dev.notmarra.inarifarms.display

import dev.notmarra.inarifarms.data.CropDataManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

class HoverDisplayManager(
    private val plugin: Plugin,
    private val dataManager: CropDataManager
) : Listener {

    private val playerDisplays = HashMap<UUID, TextDisplay>()

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
                plugin.logger.info("DISPLAY: Found crop ${cropState.cropTypeId} at ${targetBlock.x},${targetBlock.y},${targetBlock.z}")
                val displayLoc = targetBlock.location.add(0.5, 1.2, 0.5)
                val cropName = if (cropState.cropTypeId == "inari:tomato") "Rajče" else "Neznámá plodina"

                val text = Component.empty()
                    .append(Component.text("❀ ", NamedTextColor.RED))
                    .append(Component.text(cropName, NamedTextColor.WHITE))
                    .append(Component.text(" ❀", NamedTextColor.RED))
                    .append(Component.newline())
                    .append(Component.text("Vlhkost: ", NamedTextColor.GRAY))
                    .append(Component.text("${cropState.currentMoisture}%", NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(Component.text("Růst: ", NamedTextColor.GRAY))
                    .append(Component.text("Fáze ${cropState.currentStage}/7", NamedTextColor.GREEN))

                display.teleport(displayLoc)
                display.text(text)
                return
            }
        }

        // Move display away from player's sight when not hovering over crop
        val hideLoc = player.location.clone().apply { y = y - 100 }
        if (display.location.distance(hideLoc) > 1.0) {
            display.teleport(hideLoc)
        }
    }
}