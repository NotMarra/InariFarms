package dev.notmarra.inarifarms.utils

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.notmarra.inarifarms.crops.CropRegistry
import dev.notmarra.inarifarms.items.ItemManager
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class CommandBuilder(private val cropRegistry: CropRegistry) {
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
}