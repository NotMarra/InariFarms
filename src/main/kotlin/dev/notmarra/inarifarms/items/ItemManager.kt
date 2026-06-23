package dev.notmarra.inarifarms.items

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class ItemManager(plugin: Plugin) {
    var customItemKey = NamespacedKey(plugin, "custom_item_id")

    fun createTomatoSeed(): ItemStack {
        val item = ItemStack(Material.MELON_SEEDS)
        val meta = item.itemMeta ?: return item

        meta.displayName(Component.text("Semínko rajčete", NamedTextColor.RED))
        meta.lore(listOf(
            Component.text("Zasaď na zorané pole", NamedTextColor.GRAY),
            Component.text("zde budou stats", NamedTextColor.LIGHT_PURPLE),
        ))

        meta.persistentDataContainer.set(customItemKey, PersistentDataType.STRING, "inari:tomato")
        item.itemMeta = meta
        return item
    }
}