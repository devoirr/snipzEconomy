package me.snipz.economy.management

import com.google.common.cache.CacheBuilder
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.snipz.api.APIPlugin
import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.economy.EconomyTransactionType
import me.snipz.api.economy.ICurrency
import me.snipz.api.economy.IEconomyService
import me.snipz.api.runnable.QuickRunnable
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.commands.CurrenciesCommand
import me.snipz.economy.commands.EconomyCommand
import me.snipz.economy.commands.PayCommand
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.listeners.AccountLoadListener
import me.snipz.economy.`object`.Account
import me.snipz.economy.service.EconomyService
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

object EconomyManager {

    private lateinit var plugin: EconomyPlugin
    private lateinit var database: EconomyDatabase

    /**
     * Данные хранятся в течении 20 секунд.
     * В основном нужно для PlaceholderAPI, чтобы не обращаться каждый раз к БД.
     */
    private val users = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofSeconds(20)).build<UUID, Account>()

    private val formatter = DecimalFormat("#.##").apply { this.roundingMode = RoundingMode.CEILING }

    private lateinit var accountLoadListener: AccountLoadListener

    fun onEnable(plugin: EconomyPlugin, database: EconomyDatabase) {

        this.plugin = plugin
        this.database = database

        CurrenciesManager.onEnable(this.database)

        this.accountLoadListener = AccountLoadListener(this)
        this.accountLoadListener.register()

        this.plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            CurrenciesCommand(plugin).register(commands.registrar())
            EconomyCommand(plugin).register(commands.registrar())

            PayCommand(plugin).register(commands.registrar())
        }

        APIPlugin.instance.serviceManager.register(IEconomyService::class, EconomyService())
    }

    /**
     * Подгружает юзера из БД в память.
     */
    private fun asyncLoadUser(uuid: UUID) {
        QuickRunnable.runAsync {
            database.getAccount(uuid, plugin.serverId).thenAccept { account ->
                users.put(uuid, account)
            }
        }
    }

    /**
     * Создает пустого юзера для плейсхолдеров и прочего...
     * Завершив подгрузку настоящих данных, перезаписывает.
     */
    fun asyncLoadOnJoin(uuid: UUID) {
        users.put(uuid, Account(uuid, mutableMapOf(), false))
        asyncLoadUser(uuid)
    }

    @Deprecated(
        "Not recommended, use getAccountFromDatabase() instead.",
        ReplaceWith("getAccountFromDatabase(uuid)"),
        DeprecationLevel.WARNING
    )
    fun forceAccount(uuid: UUID): Account {
        var account = users.getIfPresent(uuid)

        if (account != null)
            return account

        account = database.getAccount(uuid, plugin.serverId).join()!!
        users.put(uuid, account)

        return account
    }

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

    fun formatDouble(double: Double): Double {
        return formatter.format(double).toDouble()
    }

    fun formatDoubleToString(double: Double): String = formatter.format(double)
}