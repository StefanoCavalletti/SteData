package com.example.stedata.models

data class Rilevazione(
    val timestamp: String = "",
    val machineId: String = "",

    // Campi Manuali
    val incasso: Double? = null,
    val resti: Double? = null,
    val file: String? = null, // Vecchio campo per nome file o note

    // Campi file EVA-DTS
    val fileId: String? = null,
    val incassoTotale: Double? = null,
    val numeroVendite: Int = 0,

    val infoMacchina: Map<String, String>? = null,
    val contabilitaCash: Map<String, Double>? = null,
    val contabilitaCashless: Map<String, Double>? = null,
    val prodotti: List<Map<String, Any>>? = null
) {
    // Incasso (unifica manuale e automatico)
    fun getImporto(): Double {
        return incassoTotale ?: incasso ?: 0.0
    }

    // Per distinguere file da inserimenti a mano
    fun isEvaDts(): Boolean {
        return !prodotti.isNullOrEmpty() || fileId != null
    }
}