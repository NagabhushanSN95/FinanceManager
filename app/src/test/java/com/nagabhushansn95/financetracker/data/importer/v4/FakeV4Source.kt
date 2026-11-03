package com.nagabhushansn95.financetracker.data.importer.v4

/** Hand-built v4 rows covering every mapping branch. Account/category ids mirror the real layout (Bank16 = Investments). */
class FakeV4Source : V4Source {
    private var nextId = 0
    private val txs = ArrayList<V4Transaction>()

    fun tx(
        date: String,
        type: String,
        particulars: String,
        amount: Double,
        rate: Double = 0.0,
        quantity: Double = 1.0,
        hidden: Boolean = false
    ): V4Transaction {
        val id = ++nextId
        val t = V4Transaction(
            id,
            "$date/10/00/00/$id",
            "$date/10/00/00/$id",
            date,
            type,
            particulars,
            rate,
            quantity,
            amount,
            hidden,
            true
        )
        txs += t
        return t
    }

    override fun wallets() = listOf(
        V4Wallet(1, "Wallet", 5400.0, false),
        V4Wallet(9, "Amazon Pay", 1059.56, false),
        V4Wallet(17, "Wallet USD", 0.0, true),
    )

    override fun banks() = listOf(
        V4Bank(2, "State Bank Of India", "1234", 30841.6, false),
        V4Bank(3, "HDFC Bank", "0014", 38335.39, false),
        V4Bank(5, "HDFC Millenia Credit Card", "5459", -30000.03, false),
        V4Bank(16, "Investments", "0000", 17190526.6, false),
        V4Bank(17, "Zerodha Demat", "0000", 108228.9, false),
    )

    override fun expenditureTypes() = listOf(
        V4ExpenditureType(2, "Food", false),
        V4ExpenditureType(11, "Others", false),
        V4ExpenditureType(20, "Trading", false),
    )

    override fun transactions(): List<V4Transaction> = txs
    override fun templates() = listOf(
        V4Template(1, "Good Day Biscuit", "Debit Wallet01 Exp02", 25.0, false),
        V4Template(2, "Bought Stocks -", "Transfer Bank17 Bank16", 1.0, false),
        V4Template(3, "Profit In Selling Stocks -", "Credit Bank16", 1.0, false),
        V4Template(
            4,
            "Invested In Public Provident Fund - Self",
            "Transfer Bank02 Bank16",
            12500.0,
            false
        ),
    )

    override fun close() {}
}
