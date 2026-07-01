package dev.notmarra.inarifarms.stations

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

enum class StationType {
    GROWTH,
    MIXING
}

@ConfigSerializable
data class StationBlock(
    val material: String = "BARREL",
)

@ConfigSerializable
data class Station(
    val id: String = "",
    val type: StationType = StationType.GROWTH,
    val displayName: String = "",
    val lore: List<String> = listOf(),
    val block: StationBlock = StationBlock(),
    val levels: List<StationLevel> = listOf(),
    val gui: StationGui = StationGui()
) {
    val fullId: String
        get() = if (id.startsWith("inari:")) id else "inari:$id"

    fun getLevel(level: Int): StationLevel? = levels.find { it.level == level }
    fun maxLevel(): Int = levels.maxOf { it.level }
}

@ConfigSerializable
data class StationLevel(
    val level: Int = 1,
    val storageSlots: Int = 9,
    val growthSpeedMultiplier: Float = 1.0f,
)

@ConfigSerializable
data class StationGui(
    val structure: List<String> = listOf(),
    val title: String = "<dark_gray>Station",
    val items: Map<String, StationGuiItem> = emptyMap()
)

@ConfigSerializable
data class StationGuiItem(
    val material: String = "BLACK_STAINED_GLASS_PANE",
    @Setting("base-64")
    val base64: String? = null,
    val name: String = "",
    val lore: List<String> = listOf(),
    val itemModel: String? = null
)