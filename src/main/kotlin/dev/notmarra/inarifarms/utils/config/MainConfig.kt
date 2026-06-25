package dev.notmarra.inarifarms.utils.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MainConfig(
    val debug: Boolean = false,
    val prefix: String = "<gradient:#59ff00:#37db00><b>InariFarms <dark_gray>» <white>",
    val displayText: List<String> = emptyList(),
)
