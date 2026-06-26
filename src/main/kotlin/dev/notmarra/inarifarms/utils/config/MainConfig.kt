package dev.notmarra.inarifarms.utils.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class MainConfig(
    val debug: Boolean = false,
    val prefix: String = "<gradient:#59ff00:#37db00><b>InariFarms <dark_gray>» <white>",
)
