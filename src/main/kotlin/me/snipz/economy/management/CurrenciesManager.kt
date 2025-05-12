package me.snipz.economy.management

import me.snipz.economy.api.ICurrency
import me.snipz.economy.database.EconomyDatabase

object CurrenciesManager {

    private lateinit var economyDatabase: EconomyDatabase

    data class Currency(val name: String, var global: Boolean, var symbol: String) : ICurrency {
        override fun name(): String {
            return name
        }

        override fun global(): Boolean {
            return global
        }
    }

    private val currencies = mutableMapOf<String, Currency>()

    fun onEnable(economyDatabase: EconomyDatabase) {
        this.economyDatabase = economyDatabase

        economyDatabase.getAllCurrencies().thenAccept { list ->
            list.forEach { currency ->
                currencies[currency.name] = currency
            }
        }
    }

    fun isCurrency(name: String): Boolean {
        return currencies.containsKey(name)
    }

    fun getCurrency(name: String) = currencies[name]

    fun get() = currencies.values

    fun delete(name: String) {
        currencies.remove(name)
    }

    fun add(name: String, currency: Currency) {
        currencies[name] = currency
    }

    fun publish() {
        economyDatabase.publishCurrencies(currencies.values.toList())
    }
}