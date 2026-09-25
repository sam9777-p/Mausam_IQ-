package com.hazardiqplus.data

enum class PersonaType(val id: String, val displayName: String) {
    HEALTH("health", "Health & Allergy"),
    FITNESS("fitness", "Outdoor Fitness"),
    BEACH("beach", "Beach & Surf"),
    TRAVEL("travel", "Traveler"),
    PARENT("parent", "Family & School"),
    AGRO("agro", "Agriculture"),
    COMMUTE("commute", "Commuter"),
    EVENT("event", "Event Planner")
}