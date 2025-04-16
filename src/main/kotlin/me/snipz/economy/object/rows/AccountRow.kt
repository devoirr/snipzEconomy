package me.snipz.economy.`object`.rows

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

@DatabaseTable(tableName = "accounts")
class AccountRow {

    @DatabaseField(columnName = "uuid", uniqueCombo = true, canBeNull = false, dataType = DataType.UUID)
    lateinit var uuid: UUID

    @DatabaseField(columnName = "currency", uniqueCombo = true, canBeNull = false)
    lateinit var currency: String

    @DatabaseField(columnName = "server", uniqueCombo = true, canBeNull = false)
    lateinit var server: String

    @DatabaseField(columnName = "amount", dataType = DataType.DOUBLE)
    var amount: Double = 0.0

}