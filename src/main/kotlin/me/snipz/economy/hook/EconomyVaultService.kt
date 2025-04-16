package me.snipz.economy.hook

import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.economy.EconomyTransactionType
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import net.milkbowl.vault.economy.Economy
import net.milkbowl.vault.economy.EconomyResponse
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class EconomyVaultService(private val currencyName: String) : Economy {

    override fun isEnabled(): Boolean {
        return true
    }

    override fun getName(): String {
        return "snipzEconomy"
    }

    override fun hasBankSupport(): Boolean {
        return false
    }

    override fun fractionalDigits(): Int {
        return 2
    }

    override fun format(p0: Double): String {
        return EconomyManager.formatDoubleToString(p0)
    }

    override fun currencyNamePlural(): String {
        return "dollars"
    }

    override fun currencyNameSingular(): String {
        return "dollar"
    }

    override fun hasAccount(p0: String?): Boolean {
        return true
    }

    override fun hasAccount(p0: OfflinePlayer?): Boolean {
        return true
    }

    override fun hasAccount(p0: String?, p1: String?): Boolean {
        return true
    }

    override fun hasAccount(p0: OfflinePlayer?, p1: String?): Boolean {
        return true
    }

    override fun getBalance(p0: String?): Double {
        return EconomyManager.forceAccount(Bukkit.getOfflinePlayer(p0!!).uniqueId).getBalance(currencyName)
    }

    override fun getBalance(p0: OfflinePlayer?): Double {
        return EconomyManager.forceAccount(p0!!.uniqueId).getBalance(currencyName)
    }

    override fun getBalance(p0: String?, p1: String?): Double {
        return getBalance(p0)
    }

    override fun getBalance(p0: OfflinePlayer?, p1: String?): Double {
        return getBalance(p0)
    }

    override fun has(p0: String?, p1: Double): Boolean {
        return getBalance(p0) >= p1
    }

    override fun has(p0: OfflinePlayer?, p1: Double): Boolean {
        return getBalance(p0) > p1
    }

    override fun has(p0: String?, p1: String?, p2: Double): Boolean {
        return getBalance(p0) > p2
    }

    override fun has(p0: OfflinePlayer?, p1: String?, p2: Double): Boolean {
        return getBalance(p0) > p2
    }

    override fun withdrawPlayer(p0: String?, p1: Double): EconomyResponse {
        val uuid = Bukkit.getOfflinePlayer(p0!!).uniqueId

        val response = EconomyManager.tryTransaction(
            uuid,
            CurrenciesManager.getCurrency(currencyName)!!,
            p1,
            EconomyTransactionType.TAKE
        ).join()

        val economyResponse = EconomyResponse(
            p1,
            EconomyManager.forceAccount(uuid).getBalance(currencyName),
            if (response == EconomyTransactionResponse.SUCCESS) EconomyResponse.ResponseType.SUCCESS else EconomyResponse.ResponseType.FAILURE,
            ""
        )

        return economyResponse
    }

    override fun withdrawPlayer(p0: OfflinePlayer?, p1: Double): EconomyResponse {
        return withdrawPlayer(p0!!.name, p1)
    }

    override fun withdrawPlayer(p0: String?, p1: String?, p2: Double): EconomyResponse {
        return withdrawPlayer(p0, p2)
    }

    override fun withdrawPlayer(p0: OfflinePlayer?, p1: String?, p2: Double): EconomyResponse {
        return withdrawPlayer(p0, p2)
    }

    override fun depositPlayer(p0: String?, p1: Double): EconomyResponse {
        val uuid = Bukkit.getOfflinePlayer(p0!!).uniqueId

        val response = EconomyManager.tryTransaction(
            uuid,
            CurrenciesManager.getCurrency(currencyName)!!,
            p1,
            EconomyTransactionType.ADD
        ).join()

        val economyResponse = EconomyResponse(
            p1,
            EconomyManager.forceAccount(uuid).getBalance(currencyName),
            if (response == EconomyTransactionResponse.SUCCESS) EconomyResponse.ResponseType.SUCCESS else EconomyResponse.ResponseType.FAILURE,
            ""
        )

        return economyResponse
    }

    override fun depositPlayer(p0: OfflinePlayer?, p1: Double): EconomyResponse {
        return depositPlayer(p0!!.name, p1)
    }

    override fun depositPlayer(p0: String?, p1: String?, p2: Double): EconomyResponse {
        return depositPlayer(p0, p2)
    }

    override fun depositPlayer(p0: OfflinePlayer?, p1: String?, p2: Double): EconomyResponse {
        return depositPlayer(p0, p2)
    }

    override fun createBank(p0: String?, p1: String?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun createBank(p0: String?, p1: OfflinePlayer?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun deleteBank(p0: String?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun bankBalance(p0: String?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun bankHas(p0: String?, p1: Double): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun bankWithdraw(p0: String?, p1: Double): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun bankDeposit(p0: String?, p1: Double): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun isBankOwner(p0: String?, p1: String?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun isBankOwner(p0: String?, p1: OfflinePlayer?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun isBankMember(p0: String?, p1: String?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun isBankMember(p0: String?, p1: OfflinePlayer?): EconomyResponse {
        TODO("Not yet implemented")
    }

    override fun getBanks(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun createPlayerAccount(p0: String?): Boolean {
        return true
    }

    override fun createPlayerAccount(p0: OfflinePlayer?): Boolean {
        return true
    }

    override fun createPlayerAccount(p0: String?, p1: String?): Boolean {
        return true
    }

    override fun createPlayerAccount(p0: OfflinePlayer?, p1: String?): Boolean {
        return true
    }
}