package me.snipz.economy.hook

import me.snipz.eco.EconomyResponse
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import java.util.*
import kotlin.math.absoluteValue

class EconomyServiceHook(private val database: EconomyDatabase, private val serverId: String) :
    me.snipz.eco.EconomyService {

    override suspend fun depositPlayer(
        player: UUID,
        currency: String,
        amount: Double
    ): EconomyResponse {
        val currency = CurrenciesManager.getCurrency(currency) ?: return EconomyResponse.UNKNOWN_CURRENCY
        val amount = amount.absoluteValue

        val result = EconomyManager.tryTransaction(player, currency, amount, EconomyTransactionType.ADD)
        val response = when (result) {
            EconomyTransactionResponse.SUCCESS -> {
                EconomyResponse.SUCCESS
            }

            EconomyTransactionResponse.AMOUNT_NEGATIVE -> {
                EconomyResponse.SQL_ERROR
            }

            EconomyTransactionResponse.EXCEPTION -> {
                EconomyResponse.SQL_ERROR
            }

            EconomyTransactionResponse.NOTHING_CHANGED -> {
                EconomyResponse.NOTHING_CHANGED
            }
        }

        return response
    }

    override suspend fun getBalance(
        player: UUID,
        currency: String
    ): Double {
        val currency = CurrenciesManager.getCurrency(currency) ?: return 0.0

        val account = database.getSuspendAccount(player, serverId)
        return account.getBalance(currency.name)
    }

    override suspend fun withdrawPlayer(
        player: UUID,
        currency: String,
        amount: Double
    ): EconomyResponse {
        val currency = CurrenciesManager.getCurrency(currency) ?: return EconomyResponse.UNKNOWN_CURRENCY
        val amount = amount.absoluteValue

        val result = EconomyManager.tryTransaction(player, currency, amount, EconomyTransactionType.TAKE)
        val response = when (result) {
            EconomyTransactionResponse.SUCCESS -> {
                EconomyResponse.SUCCESS
            }

            EconomyTransactionResponse.AMOUNT_NEGATIVE -> {
                EconomyResponse.SQL_ERROR
            }

            EconomyTransactionResponse.EXCEPTION -> {
                EconomyResponse.SQL_ERROR
            }

            EconomyTransactionResponse.NOTHING_CHANGED -> {
                EconomyResponse.NOTHING_CHANGED
            }
        }

        return response
    }

}