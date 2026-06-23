package dev.notmarra.inarifarms

import dev.notmarra.inarifarms.data.CropDataManager
import dev.notmarra.inarifarms.display.HoverDisplayManager
import dev.notmarra.inarifarms.items.ItemManager
import dev.notmarra.inarifarms.listeners.CropPlantListener
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class Inarifarms : JavaPlugin() {

    private lateinit var displayManager: HoverDisplayManager

    override fun onEnable() {
        val dataManager = CropDataManager(this)
        val itemManager = ItemManager(this)

        displayManager = HoverDisplayManager(this, dataManager)

        server.pluginManager.registerEvents(CropPlantListener(dataManager, itemManager), this)

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register("giverajce", object : BasicCommand {
                override fun execute(source: CommandSourceStack, args: Array<String>) {
                    val sender = source.sender
                    if (sender is Player) {
                        sender.inventory.addItem(itemManager.createTomatoSeed())
                        sender.sendMessage(Component.text("Dostal jsi Semínko Rajčete!", NamedTextColor.GREEN))
                    }
                }

                override fun permission(): String = "inarifarms.admin"
            })
        }

        logger.info("Inarifarms byl uspesne nacten!")
    }

    override fun onDisable() {
        if (::displayManager.isInitialized) {
            displayManager.cleanup()
        }
        logger.info("Inarifarms byl uspesne vypnut!")
    }
}