package com.hazardiqplus.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hazardiqplus.R

// Domain models computed directly on the client
sealed class PersonaCardItem {
    data class HealthCard(
        val aqi: Int,
        val pm25: Double,
        val pm10: Double,
        val uvIndex: Double,
        val pollenRisk: String,
        val advisory: String
    ) : PersonaCardItem()

    data class FitnessCard(
        val sunrise: String,
        val sunset: String,
        val bestHours: String,
        val heatLevel: String
    ) : PersonaCardItem()

    data class MarineCard(
        val waveHeight: Double,
        val wavePeriod: Double,
        val tideStatus: String
    ) : PersonaCardItem()

    data class CommuteCard(
        val visibilityKm: Double,
        val isFogAlert: Boolean,
        val impactMessage: String
    ) : PersonaCardItem()

    data class AgroCard(
        val frostWarning: Boolean,
        val advice: String
    ) : PersonaCardItem()

    data class EventCard(
        val comfortScore: String,
        val rainRisk: String
    ) : PersonaCardItem()
}

class PersonaDashboardAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<PersonaCardItem> = emptyList()

    companion object {
        private const val TYPE_HEALTH = 1
        private const val TYPE_FITNESS = 2
        private const val TYPE_MARINE = 3
        private const val TYPE_COMMUTE = 4
        private const val TYPE_AGRO = 5
        private const val TYPE_EVENT = 6
    }

    fun updateItems(newItems: List<PersonaCardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PersonaCardItem.HealthCard -> TYPE_HEALTH
            is PersonaCardItem.FitnessCard -> TYPE_FITNESS
            is PersonaCardItem.MarineCard -> TYPE_MARINE
            is PersonaCardItem.CommuteCard -> TYPE_COMMUTE
            is PersonaCardItem.AgroCard -> TYPE_AGRO
            is PersonaCardItem.EventCard -> TYPE_EVENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEALTH -> HealthViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            TYPE_FITNESS -> FitnessViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            TYPE_MARINE -> MarineViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            TYPE_COMMUTE -> CommuteViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            TYPE_AGRO -> AgroViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            TYPE_EVENT -> EventViewHolder(inflater.inflate(R.layout.item_card_persona_common, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PersonaCardItem.HealthCard -> (holder as HealthViewHolder).bind(item)
            is PersonaCardItem.FitnessCard -> (holder as FitnessViewHolder).bind(item)
            is PersonaCardItem.MarineCard -> (holder as MarineViewHolder).bind(item)
            is PersonaCardItem.CommuteCard -> (holder as CommuteViewHolder).bind(item)
            is PersonaCardItem.AgroCard -> (holder as AgroViewHolder).bind(item)
            is PersonaCardItem.EventCard -> (holder as EventViewHolder).bind(item)
        }
    }

    override fun getItemCount() = items.size

    // Universal clean card viewholder
    class HealthViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.HealthCard) {
            tvTitle.text = "Health & Allergy Advisory"
            tvPrimary.text = "AQI: ${item.aqi} | PM2.5: ${item.pm25} | UV: ${item.uvIndex}"
            tvSecondary.text = "${item.pollenRisk} • ${item.advisory}"
        }
    }

    class FitnessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.FitnessCard) {
            tvTitle.text = "Outdoor Fitness"
            tvPrimary.text = "Best Running Hours: ${item.bestHours}"
            tvSecondary.text = "Sunrise: ${item.sunrise} | Sunset: ${item.sunset} | Heat: ${item.heatLevel}"
        }
    }

    class MarineViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.MarineCard) {
            tvTitle.text = "Beach & Surf Conditions"
            tvPrimary.text = "Wave Height: ${item.waveHeight} m (Period: ${item.wavePeriod}s)"
            tvSecondary.text = "Status: ${item.tideStatus}"
        }
    }

    class CommuteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.CommuteCard) {
            tvTitle.text = "Commute & Travel Outlook"
            tvPrimary.text = "Road Visibility: ${item.visibilityKm} km"
            tvSecondary.text = item.impactMessage
        }
    }

    class AgroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.AgroCard) {
            tvTitle.text = "Agriculture & Gardening"
            tvPrimary.text = if (item.frostWarning) "Frost Alert Active!" else "No Frost Danger"
            tvSecondary.text = item.advice
        }
    }

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        private val tvPrimary: TextView = view.findViewById(R.id.tvCardPrimary)
        private val tvSecondary: TextView = view.findViewById(R.id.tvCardSecondary)

        fun bind(item: PersonaCardItem.EventCard) {
            tvTitle.text = "Event Planner Comfort Index"
            tvPrimary.text = "Comfort Score: ${item.comfortScore}"
            tvSecondary.text = item.rainRisk
        }
    }
}