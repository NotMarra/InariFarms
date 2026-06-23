package dev.notmarra.inarifarms.crops

class TomatoCrop : Crop {
    override val id = "inari:tomato"
    override val displayName = "Rajče"
    override val maxGrowthStage = 7
    override val baseGrowTime = 120
    override val waterConsumption = 10
}