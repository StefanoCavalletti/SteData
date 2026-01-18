package com.example.stedata.parser

import android.content.Context
import android.net.Uri
import com.example.stedata.models.*
import java.io.BufferedReader
import java.io.InputStreamReader

class EvaDtsParser {

    /**
     * Parsa un file EVA DTS da URI
     */
    fun parseFromUri(context: Context, uri: Uri): EvaDtsReport? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = BufferedReader(InputStreamReader(inputStream)).readText()
                parseContent(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parsa il contenuto di un file EVA DTS
     */
    fun parseContent(content: String): EvaDtsReport? {
        return try {
            val lines = content.split("\r\n", "\n").filter { it.isNotBlank() }
            val segments = lines.map { parseSegment(it) }

            buildReport(segments)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converte una linea in un DataSegment
     */
    private fun parseSegment(line: String): DataSegment {
        val parts = line.split("*")
        val blockId = parts[0]
        val elements = parts.drop(1)

        return DataSegment(blockId, elements)
    }

    /**
     * Costruisce il report completo
     */
    private fun buildReport(segments: List<DataSegment>): EvaDtsReport {
        return EvaDtsReport(
            header = parseHeader(segments),
            transactionSet = parseTransactionSet(segments),
            machineInfo = parseMachineInfo(segments),
            salesData = parseSalesData(segments),
            cashData = parseCashData(segments),
            cashlessData = parseCashlessData(segments),
            products = parseProducts(segments),
            events = parseEvents(segments),
            readInfo = parseReadInfo(segments),
            recordIntegrity = parseRecordIntegrity(segments)
        )
    }

    // ==================== PARSING FUNCTIONS ====================

    private fun parseHeader(segments: List<DataSegment>): ApplicationHeader {
        val dxs = segments.find { it.blockId == "DXS" }
        return ApplicationHeader(
            communicationId = dxs?.get(0) ?: "",
            functionalId = dxs?.get(1) ?: "",
            version = dxs?.get(2) ?: "",
            transmissionControl = dxs?.get(3) ?: ""
        )
    }

    private fun parseTransactionSet(segments: List<DataSegment>): TransactionSet {
        val st = segments.find { it.blockId == "ST" }
        val se = segments.find { it.blockId == "SE" }
        return TransactionSet(
            transactionId = st?.get(0) ?: "",
            controlNumber = st?.get(1) ?: "",
            numberOfSegments = se?.get(0)?.toIntOrNull() ?: 0
        )
    }

    private fun parseMachineInfo(segments: List<DataSegment>): MachineInfo {
        val id1 = segments.find { it.blockId == "ID1" }
        return MachineInfo(
            serialNumber = id1?.get(0) ?: "",
            modelNumber = id1?.getOrNull(1),
            buildStandard = id1?.getOrNull(2),
            location = id1?.getOrNull(3),
            assetNumber = id1?.getOrNull(5)
        )
    }

    private fun parseSalesData(segments: List<DataSegment>): SalesData {
        val va1 = segments.find { it.blockId == "VA1" }
        val va2 = segments.find { it.blockId == "VA2" }
        val va3 = segments.find { it.blockId == "VA3" }

        return SalesData(
            // Paid Vends (VA1)
            paidVendValueInit = va1?.get(0)?.toDoubleOrNull()?.div(100) ?: 0.0,
            paidVendCountInit = va1?.get(1)?.toIntOrNull() ?: 0,
            paidVendValueReset = va1?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            paidVendCountReset = va1?.getOrNull(3)?.toIntOrNull(),

            // Test Vends (VA2)
            testVendValueInit = va2?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            testVendCountInit = va2?.getOrNull(1)?.toIntOrNull(),
            testVendValueReset = va2?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            testVendCountReset = va2?.getOrNull(3)?.toIntOrNull(),

            // Free Vends (VA3)
            freeVendValueInit = va3?.get(0)?.toDoubleOrNull()?.div(100) ?: 0.0,
            freeVendCountInit = va3?.get(1)?.toIntOrNull() ?: 0,
            freeVendValueReset = va3?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            freeVendCountReset = va3?.getOrNull(3)?.toIntOrNull()
        )
    }

    private fun parseCashData(segments: List<DataSegment>): CashData {
        val ca2 = segments.find { it.blockId == "CA2" }
        val ca3 = segments.find { it.blockId == "CA3" }
        val ca4 = segments.find { it.blockId == "CA4" }

        return CashData(
            // Cash Sales (CA2)
            cashSalesValueInit = ca2?.get(0)?.toDoubleOrNull()?.div(100) ?: 0.0,
            cashSalesCountInit = ca2?.get(1)?.toIntOrNull() ?: 0,
            cashSalesValueReset = ca2?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            cashSalesCountReset = ca2?.getOrNull(3)?.toIntOrNull(),

            // Cash In (CA3)
            cashInReset = ca3?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashToBoxReset = ca3?.getOrNull(1)?.toDoubleOrNull()?.div(100),
            cashToTubesReset = ca3?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            billsInReset = ca3?.getOrNull(3)?.toDoubleOrNull()?.div(100),
            cashInInit = ca3?.getOrNull(4)?.toDoubleOrNull()?.div(100),
            cashToBoxInit = ca3?.getOrNull(5)?.toDoubleOrNull()?.div(100),
            cashToTubesInit = ca3?.getOrNull(6)?.toDoubleOrNull()?.div(100),
            billsInInit = ca3?.getOrNull(7)?.toDoubleOrNull()?.div(100),

            // Cash Out (CA4)
            cashDispensedReset = ca4?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashManualDispensedReset = ca4?.getOrNull(1)?.toDoubleOrNull()?.div(100),
            cashDispensedInit = ca4?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            cashManualDispensedInit = ca4?.getOrNull(3)?.toDoubleOrNull()?.div(100)
        )
    }

    private fun parseCashlessData(segments: List<DataSegment>): CashlessData? {
        val da2 = segments.find { it.blockId == "DA2" }
        val da4 = segments.find { it.blockId == "DA4" }
        val db2 = segments.find { it.blockId == "DB2" }
        val db4 = segments.find { it.blockId == "DB4" }

        if (da2 == null && db2 == null) return null

        return CashlessData(
            // Cashless 1
            cashless1SalesValueInit = da2?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashless1SalesCountInit = da2?.getOrNull(1)?.toIntOrNull(),
            cashless1SalesValueReset = da2?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            cashless1SalesCountReset = da2?.getOrNull(3)?.toIntOrNull(),
            cashless1CreditedInit = da4?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashless1CreditedReset = da4?.getOrNull(1)?.toDoubleOrNull()?.div(100),

            // Cashless 2
            cashless2SalesValueInit = db2?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashless2SalesCountInit = db2?.getOrNull(1)?.toIntOrNull(),
            cashless2SalesValueReset = db2?.getOrNull(2)?.toDoubleOrNull()?.div(100),
            cashless2SalesCountReset = db2?.getOrNull(3)?.toIntOrNull(),
            cashless2CreditedInit = db4?.getOrNull(0)?.toDoubleOrNull()?.div(100),
            cashless2CreditedReset = db4?.getOrNull(1)?.toDoubleOrNull()?.div(100)
        )
    }

    private fun parseProducts(segments: List<DataSegment>): List<ProductData> {
        val products = mutableListOf<ProductData>()
        val pa1Segments = segments.filter { it.blockId == "PA1" }

        for (pa1 in pa1Segments) {
            val productId = pa1.get(0) ?: continue

            // Trova PA2, PA4, PA7 corrispondenti
            val pa2 = findNextSegment(segments, pa1, "PA2")
            val pa4 = findNextSegment(segments, pa1, "PA4")
            val pa7List = findAllNextSegments(segments, pa1, "PA7")

            products.add(
                ProductData(
                    productId = productId,
                    price = pa1.getOrNull(1)?.toDoubleOrNull()?.div(100),
                    productName = pa1.getOrNull(2),

                    // PA2
                    paidCountInit = pa2?.getOrNull(0)?.toIntOrNull(),
                    paidValueInit = pa2?.getOrNull(1)?.toDoubleOrNull()?.div(100),
                    paidCountReset = pa2?.getOrNull(2)?.toIntOrNull(),
                    paidValueReset = pa2?.getOrNull(3)?.toDoubleOrNull()?.div(100),

                    // PA4
                    freeCountInit = pa4?.getOrNull(0)?.toIntOrNull(),
                    freeValueInit = pa4?.getOrNull(1)?.toIntOrNull(),
                    freeCountReset = pa4?.getOrNull(2)?.toIntOrNull(),
                    freeValueReset = pa4?.getOrNull(3)?.toDoubleOrNull()?.div(100),

                    // PA7
                    salesByPayment = pa7List.mapNotNull { pa7 ->
                        ProductSalesByPayment(
                            paymentDevice = pa7.get(1) ?: return@mapNotNull null,
                            priceList = pa7.getOrNull(2)?.toIntOrNull(),
                            appliedPrice = pa7.getOrNull(3)?.toDoubleOrNull()?.div(100),
                            salesCountInit = pa7.getOrNull(4)?.toIntOrNull(),
                            salesValueInit = pa7.getOrNull(5)?.toDoubleOrNull()?.div(100),
                            salesCountReset = pa7.getOrNull(6)?.toIntOrNull(),
                            salesValueReset = pa7.getOrNull(7)?.toDoubleOrNull()?.div(100)
                        )
                    }.takeIf { it.isNotEmpty() }
                )
            )
        }

        return products
    }

    private fun parseEvents(segments: List<DataSegment>): List<EventData> {
        val events = mutableListOf<EventData>()

        // EA1 segments (eventi con timestamp)
        segments.filter { it.blockId == "EA1" }.forEach { ea1 ->
            events.add(
                EventData(
                    eventId = ea1.get(0) ?: "",
                    eventDate = ea1.getOrNull(1),
                    eventTime = ea1.getOrNull(2),
                    duration = ea1.getOrNull(3)?.toIntOrNull(),
                    countReset = null,
                    countInit = null,
                    isActive = false
                )
            )
        }

        // EA2 segments (eventi con contatori)
        segments.filter { it.blockId == "EA2" }.forEach { ea2 ->
            events.add(
                EventData(
                    eventId = ea2.get(0) ?: "",
                    eventDate = null,
                    eventTime = null,
                    duration = null,
                    countReset = ea2.getOrNull(1)?.toIntOrNull(),
                    countInit = ea2.getOrNull(2)?.toIntOrNull(),
                    isActive = ea2.getOrNull(4) == "1"
                )
            )
        }

        return events
    }

    private fun parseReadInfo(segments: List<DataSegment>): ReadInfo {
        val ea3 = segments.find { it.blockId == "EA3" }
        return ReadInfo(
            readsWithResetInit = ea3?.getOrNull(0)?.toIntOrNull(),
            readDate = ea3?.getOrNull(1),
            readTime = ea3?.getOrNull(2),
            terminalId = ea3?.getOrNull(3),
            lastReadDate = ea3?.getOrNull(4),
            lastReadTime = ea3?.getOrNull(5),
            lastTerminalId = ea3?.getOrNull(6),
            totalReads = ea3?.getOrNull(8)?.toIntOrNull(),
            totalResets = ea3?.getOrNull(9)?.toIntOrNull()
        )
    }

    private fun parseRecordIntegrity(segments: List<DataSegment>): String {
        return segments.find { it.blockId == "G85" }?.get(0) ?: ""
    }

    // funzioni helper

    private fun findNextSegment(
        segments: List<DataSegment>,
        after: DataSegment,
        blockId: String
    ): DataSegment? {
        val startIndex = segments.indexOf(after)
        if (startIndex == -1) return null

        for (i in (startIndex + 1) until segments.size) {
            if (segments[i].blockId == blockId) return segments[i]
            if (segments[i].blockId.startsWith("PA1") ||
                segments[i].blockId in listOf("EA1", "EA2", "G85", "SE")) break
        }
        return null
    }

    private fun findAllNextSegments(
        segments: List<DataSegment>,
        after: DataSegment,
        blockId: String
    ): List<DataSegment> {
        val result = mutableListOf<DataSegment>()
        val startIndex = segments.indexOf(after)
        if (startIndex == -1) return result

        for (i in (startIndex + 1) until segments.size) {
            if (segments[i].blockId == blockId) {
                result.add(segments[i])
            } else if (segments[i].blockId.startsWith("PA1") ||
                segments[i].blockId in listOf("EA1", "EA2", "G85", "SE")) {
                break
            }
        }
        return result
    }

    /**
     * Classe interna per rappresentare un segmento
     */
    private data class DataSegment(
        val blockId: String,
        val elements: List<String>
    ) {
        fun get(index: Int): String? = elements.getOrNull(index)?.takeIf { it.isNotBlank() }
        fun getOrNull(index: Int): String? = get(index)
    }
}