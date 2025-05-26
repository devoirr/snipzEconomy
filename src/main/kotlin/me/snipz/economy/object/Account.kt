package me.snipz.economy.`object`

import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.toComponent
import net.kyori.adventure.text.Component
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

data class Account(val uuid: UUID, val balances: MutableMap<String, Double>, val loaded: Boolean = true) {

    companion object {
        private val formatter = DecimalFormat("#.##").apply { this.roundingMode = RoundingMode.CEILING }

        fun formatDouble(double: Double): Double {
            return formatter.format(double).toDouble()
        }

        fun formatDoubleToString(double: Double): String = formatter.format(double)
    }

    fun getBalance(currency: String) = balances[currency] ?: 0.0

    fun getPlaceholderBalance(currency: String): String {
        if (loaded)
            return formatDoubleToString(getBalance(currency))
        return "乚"
    }

    fun getBalanceComponent(): Component {
        return getBalanceString().toComponent()
    }

    fun getBalanceString(): String {
        var builder = StringBuilder()

        balances.filter { CurrenciesManager.getCurrency(it.key) != null }.onEachIndexed { index, balance ->
            val format = formatDoubleToString(balance.value) + CurrenciesManager.getCurrency(balance.key)!!.symbol
            builder = builder.append(format)

            if (index != balances.count() - 1) {
                builder.append(" ")
            }
        }

        return builder.toString()
    }

    fun acceptTransaction(type: EconomyTransactionType, currency: String, amount: Double) {
        when (type) {
            EconomyTransactionType.ADD -> {
                balances[currency] = balances.getOrDefault(currency, 0.0) + amount
            }

            EconomyTransactionType.SET -> {
                balances[currency] = amount
            }

            EconomyTransactionType.TAKE -> {
                balances[currency] = (balances.getOrDefault(currency, 0.0) - amount).coerceAtLeast(0.0)
            }
        }
    }
}