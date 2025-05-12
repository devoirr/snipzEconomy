package me.snipz.economy.database

import com.google.common.cache.CacheBuilder
import me.snipz.database.GeneralDatabase
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.api.ICurrency
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.`object`.Account
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture


class EconomyDatabase(
    private val plugin: Plugin,
    private val database: GeneralDatabase,
    cacheTime: Int
) {

    private val cache =
        CacheBuilder.newBuilder().concurrencyLevel(4).expireAfterWrite(Duration.ofSeconds(cacheTime.toLong()))
            .build<UUID, Account>()

    init {
        database.update(
            "create table if not exists currencies(name varchar(32) primary key, global bool, symbol varchar(16));"
        )

        database.update(
            "create table if not exists accounts (player varchar(36), server varchar(36), currency varchar(32), amount double, primary key (player, server, currency));"
        )
    }

    fun getAllCurrencies(): CompletableFuture<List<CurrenciesManager.Currency>> {
        return CompletableFuture.supplyAsync {
            val list = mutableListOf<CurrenciesManager.Currency>()
            database.query("select * from currencies;", { resultSet ->
                while (resultSet.next()) {
                    list.add(
                        CurrenciesManager.Currency(
                            resultSet.getString("name"),
                            resultSet.getBoolean("global"),
                            resultSet.getString("symbol"),
                        )
                    )
                }
            })

            return@supplyAsync list
        }
    }

    fun publishCurrencies(list: List<CurrenciesManager.Currency>) {
        object : BukkitRunnable() {
            override fun run() {
                database.update("delete from currencies;")

                for (currency in list) {
                    database.update(
                        "insert into currencies values(?,?,?)",
                        currency.name,
                        currency.global,
                        currency.symbol
                    )
                }
            }
        }.runTaskAsynchronously(plugin)
    }

    fun getOrGetEmptyAndLoad(uuid: UUID, server: String): Account {
        cache.getIfPresent(uuid)?.let {
            return it
        }

        val account = Account(uuid, mutableMapOf(), false)

        object : BukkitRunnable() {
            override fun run() {
                getAccount(uuid, server).thenAccept { a ->
                    cache.put(uuid, account)
                }
            }
        }.runTaskAsynchronously(plugin)

        return account
    }

    fun getAccount(uuid: UUID, serverId: String): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            try {
                val balances = mutableMapOf<String, Double>()
                database.query(
                    "select currency, amount from accounts where player=? and (server=? or server='global')",
                    { resultSet ->
                        while (resultSet.next()) {
                            balances[resultSet.getString("currency")] = resultSet.getDouble("amount")
                        }
                    },
                    uuid.toString(),
                    serverId
                )

                CurrenciesManager.get().forEach { currency ->
                    if (!balances.containsKey(currency.name)) {
                        balances[currency.name] = 0.0
                    }
                }

                val account = Account(uuid, balances)
                cache.put(uuid, account)

                return@supplyAsync account
            } catch (e: Exception) {
                e.printStackTrace()
                return@supplyAsync Account(uuid, mutableMapOf(), true)
            }
        }
    }

    fun tryTransaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        type: EconomyTransactionType,
        server: String
    ): CompletableFuture<EconomyTransactionResponse> {
        if (amount < 0) {
            return CompletableFuture.supplyAsync {
                return@supplyAsync EconomyTransactionResponse.AMOUNT_NEGATIVE
            }
        }

        return CompletableFuture.supplyAsync {
            try {
                val expression = when (type) {
                    EconomyTransactionType.TAKE -> {
                        "amount - $amount"
                    }

                    EconomyTransactionType.ADD -> {
                        "amount + $amount"
                    }

                    EconomyTransactionType.SET -> {
                        "$amount"
                    }
                }

                val server = if (currency.global()) "global" else server
                val where = if (type == EconomyTransactionType.TAKE) " and amount>=$amount;" else ""

                val sql =
                    "update accounts set amount=$expression where player=? and server=? and currency=?$where"

                val updated = database.update(
                    sql,
                    uuid.toString(),
                    server,
                    currency.name(),
                )

                if (updated == 0)
                    return@supplyAsync EconomyTransactionResponse.NOTHING_CHANGED

                cache.invalidate(uuid)

                return@supplyAsync EconomyTransactionResponse.SUCCESS
            } catch (e: Exception) {
                e.printStackTrace()
                return@supplyAsync EconomyTransactionResponse.EXCEPTION
            }
        }
    }

    fun transfer(
        sender: UUID,
        receiver: UUID,
        amount: Double,
        currency: ICurrency,
        server: String
    ): CompletableFuture<EconomyTransactionResponse> {
        if (amount < 0) {
            return CompletableFuture.supplyAsync {
                return@supplyAsync EconomyTransactionResponse.AMOUNT_NEGATIVE
            }
        }

        return CompletableFuture.supplyAsync {
            val updated =
                database.update(
                    "update accounts set amount=amount-$amount where amount >= $amount and currency=? and player = ? and (server = 'global' or server=?);",
                    currency.name(),
                    sender.toString(),
                    server
                )

            if (updated == 0) {
                return@supplyAsync EconomyTransactionResponse.NOTHING_CHANGED
            }

            database.update(
                "update accounts set amount=amount-$amount where currency=? and player = ? and (server = 'global' or server=?);",
                currency.name(),
                receiver.toString(),
                server
            )

            cache.invalidate(receiver)
            cache.invalidate(sender)

            return@supplyAsync EconomyTransactionResponse.SUCCESS
        }
    }

    fun createAccount(
        uuid: UUID,
        server: String
    ) {
        object : BukkitRunnable() {
            override fun run() {
                CurrenciesManager.get().forEach { currency ->
                    var exists = false
                    database.query("select * from accounts where player=? and server=? and currency=?", { rs ->
                        exists = rs.next()
                    }, uuid.toString(), if (currency.global) "global" else server, currency.name)

                    if (!exists)
                        database.update(
                            "insert into accounts(player, server, currency, amount) values (?,?,?,?)",
                            uuid.toString(),
                            if (currency.global) "global" else server,
                            currency.name,
                            0.0
                        )
                }
            }
        }.runTaskAsynchronously(plugin)
    }
}