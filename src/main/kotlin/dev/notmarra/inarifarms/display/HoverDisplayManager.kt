package dev.notmarra.inarifarms.display

import dev.notmarra.inarifarms.Inarifarms
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class HoverDisplayManager(
    private val plugin: Inarifarms,
    private val dataManager: CropDataManager,
    private val cropRegistry: CropRegistry
) : Listener {

    private val playerDisplays = HashMap<UUID, TextDisplay>()
    private val lastLookedState = HashMap<UUID, Pair<Location, Int>?>()
    private val lastUpdateMs = HashMap<UUID, Long>()

    private val conf = plugin.configManager.config
    private val mm = MiniMessage.miniMessage()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)

        for (player in Bukkit.getOnlinePlayers()) {
            spawnDisplayFor(player)
        }
    }

    fun cleanup() {
        playerDisplays.values.forEach { it.remove() }
        playerDisplays.clear()
        lastLookedState.clear()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        spawnDisplayFor(event.player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerDisplays.remove(event.player.uniqueId)?.remove()
        lastLookedState.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (!event.hasChangedOrientation()) return

        val now = System.currentTimeMillis()
        val last = lastUpdateMs[event.player.uniqueId] ?: 0L
        if (now - last < 150) return
        lastUpdateMs[event.player.uniqueId] = now

        updateHoverDisplay(event.player)
    }

    private fun spawnDisplayFor(player: Player) {
        playerDisplays[player.uniqueId]?.remove()

        val display = player.world.spawn(player.location.clone(), TextDisplay::class.java) { textDisplay ->
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

    private fun updateHoverDisplay(player: Player) {
        val display = playerDisplays[player.uniqueId] ?: return

        val targetBlock = player.getTargetBlockExact(5)
        val targetLoc = targetBlock?.location

        if (targetBlock == null || targetBlock.type.isAir) {
            lastLookedState.remove(player.uniqueId)
            hideDisplay(display, player)
            return
        }

        val cropState = dataManager.getCropData(targetBlock)

        if (cropState == null) {
            lastLookedState.remove(player.uniqueId)
            hideDisplay(display, player)
            return
        }

        val cached = lastLookedState[player.uniqueId]
        if (cached != null && cached.first == targetLoc && cached.second == cropState.currentStage) {
            return
        }
        lastLookedState[player.uniqueId] = Pair(targetLoc!!, cropState.currentStage)

        val displayLoc = targetBlock.location.add(0.5, 1.2, 0.5)
        val crop = cropRegistry.getCrop(cropState.cropTypeId)
        val cropName = crop?.displayName ?: "NaN"

        plugin.server.asyncScheduler.runNow(plugin) {
            val textLines = conf.displayText.map {
                it.replace("%displayName%", cropName)
                    .replace("%moisture%", cropState.currentMoisture.toString())
                    .replace("%stage%", cropState.currentStage.toString())
                    .replace("%finalStage%", crop?.maxGrowthStage.toString())
            }

            val finalText = textLines
                .map { mm.deserialize(it) }
                .reduce { acc, component -> acc.append(Component.newline()).append(component) }

            plugin.server.regionScheduler.run(plugin, displayLoc.world, displayLoc.blockX shr 4, displayLoc.blockZ shr 4) {
                if (display.isValid && player.isOnline) {
                    display.teleport(displayLoc)
                    display.text(finalText)
                }
            }
        }
    }

    private fun hideDisplay(display: TextDisplay, player: Player) {
        val hideLoc = player.location.clone().apply { y -= 100 }
        if (display.location.distance(hideLoc) > 1.0) {
            display.teleport(hideLoc)
        }
    }
}