package dev.notmarra.inarifarms.crops

interface Crop {
    val id: String
    val displayName: String
    val maxGrowthStage: Int
    val baseGrowTime: Int
    val waterConsumption: Int
}