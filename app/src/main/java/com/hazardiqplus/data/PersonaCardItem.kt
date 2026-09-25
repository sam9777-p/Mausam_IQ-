package com.hazardiqplus.data

sealed class PersonaCardItem {

    data class HealthCard(
        val data: HealthInsight
    ) : PersonaCardItem()

    data class FitnessCard(
        val data: FitnessInsight
    ) : PersonaCardItem()

    data class MarineCard(
        val data: MarineInsight
    ) : PersonaCardItem()

    data class CommuteCard(
        val data: CommuteInsight
    ) : PersonaCardItem()

    data class AgroCard(
        val data: AgricultureInsight
    ) : PersonaCardItem()

    data class EventCard(
        val data: EventPlannerInsight
    ) : PersonaCardItem()
}