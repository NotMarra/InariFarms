package dev.notmarra.inarifarms.utils

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.notmarra.inarifarms.Inarifarms
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.data.CropDataManager
import dev.notmarra.inarifarms.items.ItemManager
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player

class CommandBuilder(plugin: Inarifarms, private val dataManager: CropDataManager, private val cropRegistry: CropRegistry) {
    private val conf = plugin.configManager.config
    fun giveSeedCommand(itemManager: ItemManager): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("giveseed")
            .requires { it.sender.hasPermission("inarifarms.admin") }
            .then(
                Commands.argument("id", StringArgumentType.word())
                    .suggests { _, builder ->
                        cropRegistry.getAllCrops().forEach { crop ->
                            builder.suggest(crop.id)
                        }
                        builder.buildFuture()
                    }
                    .executes { context ->
                        val sender = context.source.sender
                        if (sender !is Player) {
                            sender.sendMessage(Component.text("Only a player can use this command.", NamedTextColor.RED))
                            return@executes 0
                        }

                        val inputId = StringArgumentType.getString(context, "id")
                        val searchId = if (inputId.startsWith("inari:")) inputId else "inari:$inputId"
                        val crop = cropRegistry.getCrop(searchId)

                        if (crop != null) {
                            sender.inventory.addItem(itemManager.createSeed(crop))
                            sender.sendMessage(Component.text("You received the seed for crop ${crop.id}!", NamedTextColor.GREEN))
                        } else {
                            sender.sendMessage(Component.text("Crop not found!", NamedTextColor.RED))
                        }

                        1
                    }
            )
            .build()
    }

    fun debugCommand(): BasicCommand = object : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val sender = source.sender
            if (sender !is Player) return

            val mm = MiniMessage.miniMessage()

            if (!conf.debug) {
                sender.sendMessage(mm.deserialize(conf.prefix + "<red>Debug mode is currently disabled in config.yml!"))
                return
            }

            val chunk = sender.location.chunk
            val chunkCrops = dataManager.getChunkCrops(chunk)

            sender.sendMessage(mm.deserialize("<dark_gray>+---------------------------------------------------+"))
            sender.sendMessage(mm.deserialize(" <#59ff00><b>INARIFARMS DEBUG</b> <gray>| Chunk: <white>${chunk.x}, ${chunk.z} <gray>| Saved: <white>${chunkCrops.size}"))
            sender.sendMessage(mm.deserialize("<dark_gray>+---------------------------------------------------+"))

            if (chunkCrops.isEmpty()) {
                sender.sendMessage(mm.deserialize(" <gray><i>No crop data is stored in this chunk.</i>"))
            } else {
                for ((coords, state) in chunkCrops) {
                    sender.sendMessage(mm.deserialize(
                        " <dark_gray>» <white>Position: <gray>[$coords] <dark_gray>| <white>Type: <#37db00>${state.cropTypeId}"
                    ))
                    sender.sendMessage(mm.deserialize(
                        "   <gray>Stage: <white>${state.currentStage} <dark_gray>| <gray>Water: <white>${state.currentMoisture} <dark_gray>| <gray>Next growth (ms): <white>${state.nextGrowthTime}"
                    ))
                }
            }
            sender.sendMessage(mm.deserialize("<dark_gray>+---------------------------------------------------+"))
        }

        override fun permission(): String = "inarifarms.admin"
    }
}