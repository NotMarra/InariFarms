package dev.notmarra.inarifarms.crops

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class Crop(
    val id: String = "",
    val displayName: String = "",
    val maxGrowthStage: Int = 7,
    val growthTime: Int = 120,
    val waterConsumption: Int = 1,
    val seed: SeedConfig = SeedConfig(),
    val mature: MatureConfig = MatureConfig(),
) {
    val fullId: String
        get() = if (id.startsWith("inari:")) id else "inari:$id"
}

@ConfigSerializable
data class SeedConfig(
    val material: String = "WHEAT_SEEDS",
    @Setting("base-64")
    val base64: String? = null,
    val name: String = "",
    val lore: List<String> = listOf(),
    val itemModel: String? = null
)

@ConfigSerializable
data class MatureConfig(
    val material: String = "PLAYER_HEAD",
    @Setting("base-64")
    val base64: String? = null,
    val name: String = "",
    val lore: List<String> = listOf(),
    val itemModel: String? = null
)
