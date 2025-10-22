package com.example.stedata.models

data class Rilevazione(
    val timestamp: String = "",
    val incasso: Double = 0.0,
    val resti: Double = 0.0,
    val file: String = ""
)
