package com.example.stedata.models

/**
 * Report EVA DTS completo
 */
data class EvaDtsReport(
    val header: ApplicationHeader,
    val transactionSet: TransactionSet,
    val machineInfo: MachineInfo,
    val salesData: SalesData,
    val cashData: CashData,
    val cashlessData: CashlessData?,
    val products: List<ProductData>,
    val events: List<EventData>,
    val readInfo: ReadInfo,
    val recordIntegrity: String
)

/**
 * Application Header (DXS)
 */
data class ApplicationHeader(
    val communicationId: String,
    val functionalId: String,
    val version: String,
    val transmissionControl: String
)

/**
 * Transaction Set (ST/SE)
 */
data class TransactionSet(
    val transactionId: String,
    val controlNumber: String,
    val numberOfSegments: Int
)

/**
 * Identificazione Macchina (ID1)
 */
data class MachineInfo(
    val serialNumber: String,
    val modelNumber: String?,
    val buildStandard: String?,
    val location: String?,
    val assetNumber: String?
)

/**
 * Dati di vendita (VA1, VA2, VA3)
 */
data class SalesData(
    // Paid Vends
    val paidVendValueInit: Double,
    val paidVendCountInit: Int,
    val paidVendValueReset: Double?,
    val paidVendCountReset: Int?,

    // Test Vends
    val testVendValueInit: Double?,
    val testVendCountInit: Int?,
    val testVendValueReset: Double?,
    val testVendCountReset: Int?,

    // Free Vends
    val freeVendValueInit: Double,
    val freeVendCountInit: Int,
    val freeVendValueReset: Double?,
    val freeVendCountReset: Int?
)

/**
 * Dati Cash (CA2, CA3, CA4)
 */
data class CashData(
    // Cash Sales
    val cashSalesValueInit: Double,
    val cashSalesCountInit: Int,
    val cashSalesValueReset: Double?,
    val cashSalesCountReset: Int?,

    // Cash In
    val cashInReset: Double?,
    val cashToBoxReset: Double?,
    val cashToTubesReset: Double?,
    val billsInReset: Double?,
    val cashInInit: Double?,
    val cashToBoxInit: Double?,
    val cashToTubesInit: Double?,
    val billsInInit: Double?,

    // Cash Out
    val cashDispensedReset: Double?,
    val cashManualDispensedReset: Double?,
    val cashDispensedInit: Double?,
    val cashManualDispensedInit: Double?
)

/**
 * Dati Cashless (DA2, DA4, DB2, DB4)
 */
data class CashlessData(
    // Cashless 1
    val cashless1SalesValueInit: Double?,
    val cashless1SalesCountInit: Int?,
    val cashless1SalesValueReset: Double?,
    val cashless1SalesCountReset: Int?,
    val cashless1CreditedInit: Double?,
    val cashless1CreditedReset: Double?,

    // Cashless 2
    val cashless2SalesValueInit: Double?,
    val cashless2SalesCountInit: Int?,
    val cashless2SalesValueReset: Double?,
    val cashless2SalesCountReset: Int?,
    val cashless2CreditedInit: Double?,
    val cashless2CreditedReset: Double?
)

/**
 * Dati Prodotto (PA1, PA2, PA4, PA7)
 */
data class ProductData(
    val productId: String,
    val price: Double?,
    val productName: String?,

    // PA2 - Paid products
    val paidCountInit: Int?,
    val paidValueInit: Double?,
    val paidCountReset: Int?,
    val paidValueReset: Double?,

    // PA4 - Free vends
    val freeCountInit: Int?,
    val freeValueInit: Int?,
    val freeCountReset: Int?,
    val freeValueReset: Double?,

    // PA7 - Sales by payment device
    val salesByPayment: List<ProductSalesByPayment>?
)

/**
 * Vendite prodotto per tipo di pagamento (PA7)
 */
data class ProductSalesByPayment(
    val paymentDevice: String, // CA, DA, DB, TA
    val priceList: Int?,
    val appliedPrice: Double?,
    val salesCountInit: Int?,
    val salesValueInit: Double?,
    val salesCountReset: Int?,
    val salesValueReset: Double?
)

/**
 * Eventi (EA1, EA2)
 */
data class EventData(
    val eventId: String,
    val eventDate: String?,
    val eventTime: String?,
    val duration: Int?,
    val countReset: Int?,
    val countInit: Int?,
    val isActive: Boolean
)

/**
 * Informazioni Lettura (EA3)
 */
data class ReadInfo(
    val readsWithResetInit: Int?,
    val readDate: String?,
    val readTime: String?,
    val terminalId: String?,
    val lastReadDate: String?,
    val lastReadTime: String?,
    val lastTerminalId: String?,
    val totalReads: Int?,
    val totalResets: Int?
)

/**
 * Informazioni Valuta (ID4)
 */
data class CurrencyInfo(
    val decimalPosition: Int,
    val numericCode: String?,
    val alphabeticCode: String?
)