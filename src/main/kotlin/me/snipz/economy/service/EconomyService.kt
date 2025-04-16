package me.snipz.economy.service

import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.economy.EconomyTransactionType
import me.snipz.api.economy.ICurrency
import me.snipz.api.economy.IEconomyService
import me.snipz.economy.management.EconomyManager
import java.util.*
import java.util.concurrent.CompletableFuture

class EconomyService : IEconomyService {

    override fun transaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        type: EconomyTransactionType
    ): CompletableFuture<EconomyTransactionResponse> {
        return EconomyManager.tryTransaction(uuid, currency, amount, type)
    }

    override fun onEnable() {}
    override fun onDisable() {}
}