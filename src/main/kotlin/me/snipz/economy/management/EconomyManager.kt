package me.snipz.economy.management

import com.google.common.cache.CacheBuilder
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.api.ICurrency
import me.snipz.economy.commands.CurrenciesCommand
import me.snipz.economy.commands.EconomyCommand
import me.snipz.economy.commands.PayCommand
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.`object`.Account
import org.bukkit.scheduler.BukkitRunnable
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

object EconomyManager {

    private lateinit var plugin: EconomyPlugin
    private lateinit var database: EconomyDatabase

    private val formatter = DecimalFormat("#.##").apply { this.roundingMode = RoundingMode.CEILING }

    fun onEnable(plugin: EconomyPlugin, database: EconomyDatabase) {

        this.plugin = plugin
        this.database = database

        CurrenciesManager.onEnable(this.database)

        this.plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            CurrenciesCommand(plugin).register(commands.registrar())
            EconomyCommand(plugin).register(commands.registrar())

            PayCommand(plugin).register(commands.registrar())
        }
    }

    @Deprecated(
        "Not recommended, use getAccountFromDatabase() instead.",
        ReplaceWith("getAccountFromDatabase(uuid)"),
        DeprecationLevel.WARNING
    )
    fun forceAccount(uuid: UUID): Account {
        return database.getAccount(uuid, plugin.serverId).join()!!
    }

    fun getOrGetEmptyAndLoad(uuid: UUID) = database.getOrGetEmptyAndLoad(uuid, plugin.serverId)

    fun getAccountFromDatabase(uuid: UUID) = database.getAccount(uuid, serverId = plugin.serverId)

    fun tryTransaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        type: EconomyTransactionType
    ): CompletableFuture<EconomyTransactionResponse> {
        return database.tryTransaction(uuid, currency, amount, type, plugin.serverId)
    }

    fun tryTransfer(
        sender: UUID,
        receiver: UUID,
        currency: ICurrency,
        amount: Double
    ): CompletableFuture<EconomyTransactionResponse> {
        return database.transfer(sender, receiver, amount, currency, plugin.serverId)
    }
}