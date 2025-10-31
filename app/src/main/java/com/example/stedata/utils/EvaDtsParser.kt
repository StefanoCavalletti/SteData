package com.example.stedata.utils

data class EvaDtsData(
    val machineId: String? = null,
    val version: String? = null,
    val incassi: Map<String, Double> = emptyMap(),
    val resti: Map<String, Double> = emptyMap(),
    val dispenser: Map<String, Double> = emptyMap(),
    val canali: List<CanaleIncasso> = emptyList(),
    val moduli: Map<String, List<String>> = emptyMap(),
    val rawLines: List<String> = emptyList()
)

data class CanaleIncasso(
    val nome: String,
    val valore: Double,
    val quantita: Int,
    val totale: Double
)

object EvaDtsParser {

    fun parse(content: String): EvaDtsData {
        val lines = content.trim().split("\n").map { it.trim() }
        val incassi = mutableMapOf<String, Double>()
        val resti = mutableMapOf<String, Double>()
        val dispenser = mutableMapOf<String, Double>()
        val canali = mutableListOf<CanaleIncasso>()
        val moduli = mutableMapOf<String, MutableList<String>>()

        var machineId: String? = null
        var version: String? = null

        for (line in lines) {
            val parts = line.split("*")
            if (parts.isEmpty()) continue
            val prefix = parts[0].trim()

            when {
                prefix == "DXS" -> {
                    machineId = parts.getOrNull(1)
                    version = "${parts.getOrNull(2) ?: ""} ${parts.getOrNull(3) ?: ""}".trim()
                }

                prefix.startsWith("CA") -> {
                    val name = prefix
                    val values = parts.drop(1).filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull() }
                    if (values.isNotEmpty()) {
                        incassi[name] = values.last() / 100.0
                    }
                }

                prefix.startsWith("TA") -> {
                    val name = prefix
                    val values = parts.drop(1).filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull() }
                    if (values.isNotEmpty()) {
                        resti[name] = values.last() / 100.0
                    }
                }

                prefix.startsWith("DA") -> {
                    val name = prefix
                    val values = parts.drop(1).filter { it.isNotBlank() }.mapNotNull { it.toDoubleOrNull() }
                    if (values.isNotEmpty()) {
                        dispenser[name] = values.last() / 100.0
                    }
                }

                prefix.startsWith("PA") -> {
                    val name = parts.getOrNull(1) ?: "?"
                    val valore = parts.getOrNull(2)?.toDoubleOrNull()?.div(100.0) ?: 0.0
                    val quantita = parts.getOrNull(3)?.toIntOrNull() ?: 0
                    val totale = valore * quantita
                    canali.add(CanaleIncasso(nome = name, valore = valore, quantita = quantita, totale = totale))
                }

                prefix.startsWith("MA") -> {
                    val modulo = parts.getOrNull(1) ?: "?"
                    val valori = parts.drop(2)
                    moduli.getOrPut(modulo) { mutableListOf() }.addAll(valori)
                }

                else -> {} // altri codici ignorati per ora (EA, LE, LS, etc.)
            }
        }

        return EvaDtsData(
            machineId = machineId,
            version = version,
            incassi = incassi,
            resti = resti,
            dispenser = dispenser,
            canali = canali,
            moduli = moduli,
            rawLines = lines
        )
    }
}
