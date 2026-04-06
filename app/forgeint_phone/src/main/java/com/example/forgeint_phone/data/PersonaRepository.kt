package com.example.forgeint.data

interface PersonaRepository<T> {
    fun getCustomPersonas(): List<T>
    fun addCustomPersona(persona: T)
    fun deleteCustomPersona(id: String)
}
