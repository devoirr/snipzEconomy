package me.snipz.economy.`object`

import me.snipz.api.toComponent
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import net.kyori.adventure.text.Component
import java.util.*

data class Account(val uuid: UUID, val balances: MutableMap<String, Double>, val loaded: Boolean = true) {

    fun getBalance(currency: String) = balances[currency] ?: 0.0

    fun getBalanceString(): Component {
        var builder = Component.empty()

        for (entry in balances) {
            builder =
                builder.append("${EconomyManager.formatDouble(entry.value)}${CurrenciesManager.getCurrency(entry.key)!!.symbol} ".toComponent())
        }

        return builder
    }

}