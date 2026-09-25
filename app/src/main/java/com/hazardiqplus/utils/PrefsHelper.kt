package com.hazardiqplus.utils

import android.content.Context
import androidx.core.content.edit
import com.hazardiqplus.data.PersonaType

object PrefsHelper {

    private const val PREFS_NAME = "HazardIQPrefs"
    private const val KEY_ROLE = "user_role"
    private const val KEY_ACTIVE_PERSONAS = "KEY_ACTIVE_PERSONAS"

    fun saveUserRole(context: Context, role: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_ROLE, role)
        }
    }

    fun getUserRole(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE, null)
    }

    fun getActivePersonas(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return prefs.getStringSet(
            KEY_ACTIVE_PERSONAS,
            setOf(PersonaType.HEALTH.id)
        ) ?: emptySet()
    }

    fun saveActivePersonas(context: Context, personas: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            putStringSet(KEY_ACTIVE_PERSONAS, personas)
        }
    }

    fun togglePersona(
        context: Context,
        personaId: String,
        isSelected: Boolean
    ): Set<String> {

        val currentPersonas =
            getActivePersonas(context).toMutableSet()

        if (isSelected) {
            currentPersonas.add(personaId)
        } else {
            currentPersonas.remove(personaId)
        }

        // Ensure at least one persona is always active
        if (currentPersonas.isEmpty()) {
            currentPersonas.add(PersonaType.HEALTH.id)
        }

        saveActivePersonas(context, currentPersonas)

        return currentPersonas
    }
}