package com.example.forgeint.data

import android.content.Context
import android.content.SharedPreferences
import Personas
import Persona
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.content.edit

class CustomPersonaRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("custom_personas_wear", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val KEY_CUSTOM_PERSONAS = "saved_custom_personas"

    fun getCustomPersonas(): List<Persona> {
        val json = prefs.getString(KEY_CUSTOM_PERSONAS, null) ?: return emptyList()
        val type = object : TypeToken<List<Persona>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomPersona(persona: Persona) {
        val current = getCustomPersonas().toMutableList()
        current.add(persona)
        saveList(current)
    }
    
    fun deleteCustomPersona(id: String) {
        val current = getCustomPersonas().toMutableList()
        current.removeAll { it.id == id }
        saveList(current)
    }

    private fun saveList(list: List<Persona>) {
        val json = gson.toJson(list)
        prefs.edit { putString(KEY_CUSTOM_PERSONAS, json) }
    }
}
