package dev.notmarra.inarifarms.items

import com.destroystokyo.paper.profile.PlayerProfile
import com.destroystokyo.paper.profile.ProfileProperty
import dev.notmarra.inarifarms.crops.Crop
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

class ItemManager(plugin: Plugin) {
    val customItemKey = NamespacedKey(plugin, "inari_seed_item")
    private val mm = MiniMessage.miniMessage()

    fun createSeed(crop: Crop): ItemStack {
        val material = Material.matchMaterial(crop.seed.material) ?: Material.WHEAT_SEEDS
        val item = ItemStack(material)
        val meta = item.itemMeta ?: return item

        if (crop.seed.lore.isNotEmpty()) {
            meta.lore(crop.seed.lore.map { mm.deserialize(it).decoration(TextDecoration.ITALIC, false) })
        }

        meta.persistentDataContainer.set(customItemKey, PersistentDataType.STRING, crop.fullId)
        item.itemMeta = meta

        val parsedNameString = crop.seed.name.replace("%displayName%", crop.displayName)
        item.setData(DataComponentTypes.CUSTOM_NAME, mm.deserialize(parsedNameString).decoration(TextDecoration.ITALIC, false))

        if (crop.seed.itemModel != null) {
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key(crop.seed.itemModel))
        }

        if (material == Material.PLAYER_HEAD && crop.seed.base64 != null) {
            val profile: PlayerProfile = Bukkit.createProfile(UUID.randomUUID())
            profile.setProperty(ProfileProperty("textures", crop.seed.base64))

            (item.itemMeta as? SkullMeta)?.apply {
                playerProfile = profile
                item.itemMeta = this
            }
        }

        return item
    }
}