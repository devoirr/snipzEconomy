package me.snipz.economy.management

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.api.ICurrency
import me.snipz.economy.commands.EconomyCommand
import me.snipz.economy.commands.PayCommand
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.`object`.Account
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

object EconomyManager {

    private lateinit var plugin: EconomyPlugin
    private lateinit var database: EconomyDatabase

    private val loadedPlayers = mutableMapOf<UUID, Account>()
    private val savingQueue: Queue<Account> = LinkedList()

    fun onEnable(plugin: EconomyPlugin, database: EconomyDatabase) {
        this.plugin = plugin
        this.database = database

        CurrenciesManager.onEnable(plugin.config)

        object : BukkitRunnable() {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    while (savingQueue.isNotEmpty()) {
                        val account = savingQueue.remove()
                        database.saveAccount(account, plugin.serverId)
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L * 30)

        this.plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            EconomyCommand(plugin).register(commands.registrar())
            PayCommand(plugin).register(commands.registrar())
        }
    }

    fun onDisable() {
        runBlocking {
            while (savingQueue.isNotEmpty()) {
                val account = savingQueue.remove()
                database.saveAccount(account, plugin.serverId)
            }
        }
    }

    fun loadAccount(uuid: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            val account = database.getSuspendAccount(uuid, plugin.serverId)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                loadedPlayers[uuid] = account
            })
        }
    }

    fun unloadAccount(uuid: UUID) {
        loadedPlayers.remove(uuid)
    }

    suspend fun getAccount(uuid: UUID): Account {
        loadedPlayers[uuid]?.let {
            return it
        }

        return try {
            val account = database.getSuspendAccount(uuid, plugin.serverId)
            account
        } catch (e: Exception) {
            e.printStackTrace()
            Account(uuid, mutableMapOf(), false)
        }
    }

    suspend fun tryTransaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        type: EconomyTransactionType
    ): EconomyTransactionResponse {
        if (amount < 0) {
            return EconomyTransactionResponse.AMOUNT_NEGATIVE
        }

        loadedPlayers[uuid]?.let {
            if (type == EconomyTransactionType.TAKE && it.getBalance(currency.name()) < amount) {
                return EconomyTransactionResponse.NOTHING_CHANGED
            }

            it.acceptTransaction(type, currency.name(), amount)

            savingQueue.removeAll { account -> account.uuid == uuid }
            savingQueue.add(it)

            return EconomyTransactionResponse.SUCCESS
        }

        return database.tryTransaction(uuid, currency, amount, type, plugin.serverId)
    }

    suspend fun tryTransfer(
        sender: UUID,
        receiver: UUID,
        currency: ICurrency,
        amount: Double
    ): EconomyTransactionResponse {
        return database.transfer(sender, receiver, amount, currency, plugin.serverId)
    }
}