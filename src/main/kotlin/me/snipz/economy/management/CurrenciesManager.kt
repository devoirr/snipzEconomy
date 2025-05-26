package me.snipz.economy.management

import me.snipz.economy.api.ICurrency
import org.bukkit.configuration.file.FileConfiguration

object CurrenciesManager {

    data class Currency(val name: String, var global: Boolean, var symbol: String) : ICurrency {
        override fun name(): String {
            return name
        }

        override fun global(): Boolean {
            return global
        }
    }

    private val currencies = mutableMapOf<String, Currency>()

    fun onEnable(config: FileConfiguration) {
        val section = config.getConfigurationSection("currencies") ?: return
        val sections = section.getKeys(false).mapNotNull { section.getConfigurationSection(it) }

        sections.forEach { sec ->
            val name = sec.name

            val global = sec.getBoolean("global", false)
            val symbol = sec.getString("symbol", "$") ?: "$"

            currencies[name] = Currency(name, global, symbol)
        }
    }

    fun isCurrency(name: String): Boolean {
        return currencies.containsKey(name)
    }

    fun getCurrency(name: String) = currencies[name]

    fun get() = currencies.values

}