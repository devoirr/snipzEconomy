package me.snipz.economy.hook

import kotlinx.coroutines.runBlocking
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import org.bukkit.entity.Player

class EconomyPlaceholders : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "economy"
    }

    override fun getAuthor(): String {
        return "devoirr"
    }

    override fun getVersion(): String {
        return "alpha-1"
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun persist(): Boolean {
        return true
    }

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null)
            return null

        val args = params.split("_")

        if (args[0] != "balance")
            return null

        if (args.size < 2)
            return "Specify currency!"

        val currencyName = args[1]
        if (!CurrenciesManager.isCurrency(currencyName)) {
            return "Unknown currency"
        }

        val clear = args.last() == "clear"

        val account = runBlocking {
            return@runBlocking EconomyManager.getAccount(player.uniqueId)
        }

        val balance = account.getPlaceholderBalance(currencyName)

        if (clear)
            return balance

        val symbol = CurrenciesManager.getCurrency(currencyName)!!.symbol
        return "$balance$symbol"
    }
}