package me.snipz.economy.`object`.rows

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "currencies")
class CurrencyRow {

    @DatabaseField(columnName = "name", id = true, canBeNull = false)
    lateinit var name: String

    @DatabaseField(columnName = "global", dataType = DataType.BOOLEAN)
    var global: Boolean = false

    @DatabaseField(columnName = "symbol", dataType = DataType.STRING)
    lateinit var symbol: String

    @DatabaseField(columnName = "rookie", dataType = DataType.DOUBLE)
    var rookie: Double = 0.0

}