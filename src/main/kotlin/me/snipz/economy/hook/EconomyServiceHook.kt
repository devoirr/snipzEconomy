package me.snipz.economy.hook

import me.snipz.eco.EconomyResponse
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.management.CurrenciesManager
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.absoluteValue

class EconomyServiceHook(private val database: EconomyDatabase, private val serverId: String) :
    me.snipz.eco.EconomyService {

    override fun depositPlayer(
        player: UUID,
        currency: String,
        amount: Double
    ): CompletableFuture<EconomyResponse> {
        val currency = CurrenciesManager.getCurrency(currency) ?: return CompletableFuture.completedFuture(
            EconomyResponse.UNKNOWN_CURRENCY
        )
        val amount = amount.absoluteValue

        return database.tryTransaction(player, currency, amount, EconomyTransactionType.ADD, serverId)
            .thenApply {
                return@thenApply when (it) {
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
            }
    }

    override fun getBalance(
        player: UUID,
        currency: String
    ): CompletableFuture<Double> {
        val currency = CurrenciesManager.getCurrency(currency) ?: return CompletableFuture.completedFuture(0.0)
        return database.getAccount(player, serverId).thenApply { it.getBalance(currency.name) }
    }

    override fun withdrawPlayer(
        player: UUID,
        currency: String,
        amount: Double
    ): CompletableFuture<EconomyResponse> {
        val currency = CurrenciesManager.getCurrency(currency) ?: return CompletableFuture.completedFuture(
            EconomyResponse.UNKNOWN_CURRENCY
        )
        val amount = amount.absoluteValue

        return database.tryTransaction(player, currency, amount, EconomyTransactionType.TAKE, serverId)
            .thenApply {
                return@thenApply when (it) {
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
            }
    }

}