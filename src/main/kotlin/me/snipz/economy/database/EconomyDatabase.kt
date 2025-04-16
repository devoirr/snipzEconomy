package me.snipz.economy.database

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.stmt.Where
import com.j256.ormlite.table.TableUtils
import me.snipz.api.database.DatabaseInfo
import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.economy.EconomyTransactionType
import me.snipz.api.economy.ICurrency
import me.snipz.api.runnable.QuickRunnable
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.`object`.Account
import me.snipz.economy.`object`.rows.AccountRow
import me.snipz.economy.`object`.rows.CurrencyRow
import java.util.*
import java.util.concurrent.CompletableFuture


class EconomyDatabase(databaseInfo: DatabaseInfo) {

    private val currenciesDao: Dao<CurrencyRow, String>
    private val accountsDao: Dao<AccountRow, String>

    private val connectionSource = JdbcConnectionSource(databaseInfo.connectionString)

    init {
        TableUtils.createTableIfNotExists(connectionSource, CurrencyRow::class.java)
        TableUtils.createTableIfNotExists(connectionSource, AccountRow::class.java)

        this.currenciesDao = DaoManager.createDao(connectionSource, CurrencyRow::class.java)
        this.accountsDao = DaoManager.createDao(connectionSource, AccountRow::class.java)
    }

    fun getAllCurrencies(): CompletableFuture<List<CurrenciesManager.Currency>> {
        return CompletableFuture.supplyAsync {
            val list = currenciesDao.queryForAll()

            return@supplyAsync list.map { CurrenciesManager.Currency(it.name, it.global, it.symbol, it.rookie) }
        }
    }

    fun publishCurrencies(list: List<CurrenciesManager.Currency>) {
        QuickRunnable.runAsync {
            TableUtils.clearTable(connectionSource, CurrencyRow::class.java)

            for (currency in list) {
                val row = CurrencyRow().apply {
                    this.name = currency.name
                    this.symbol = currency.symbol
                    this.global = currency.global
                    this.rookie = currency.rookie
                }

                currenciesDao.create(row)
            }
        }
    }

    fun getAccount(uuid: UUID, serverId: String): CompletableFuture<Account> {
        return CompletableFuture.supplyAsync {
            try {
                val balances = mutableMapOf<String, Double>()

                val currencies = currenciesDao.queryForAll()
                for (currency in currencies) {
                    val balanceBuilder = accountsDao.queryBuilder()

                    val server = if (currency.global) "global" else serverId

                    balanceBuilder.where()
                        .eq("uuid", uuid)
                        .and()
                        .eq("currency", currency.name)
                        .and()
                        .eq("server", server)

                    val balance = balanceBuilder.queryForFirst()

                    val amount = balance?.amount ?: currency.rookie
                    balances[currency.name] = amount
                }

                return@supplyAsync Account(uuid, balances)
            } catch (e: Exception) {
                e.printStackTrace()
                return@supplyAsync Account(uuid, mutableMapOf<String, Double>(), true)
            }
        }
    }

    private fun Where<AccountRow, String>.matchServerOrGlobal(
        server: String,
        global: Boolean
    ): Where<AccountRow, String> {
        return if (global)
            this.eq("server", "global")
        else this.eq("server", server)
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
                var queryBuilder = accountsDao.queryBuilder()
                    .where()
                    .eq("uuid", uuid)
                    .and()
                    .eq("currency", currency.name())
                    .and()
                    .matchServerOrGlobal(server, currency.global())

                if (type == EconomyTransactionType.TAKE) {
                    queryBuilder = queryBuilder.and().ge("amount", amount)
                }

                var first = queryBuilder.queryForFirst()
                var isCreated = true

                if (first == null) {
                    if (type == EconomyTransactionType.TAKE) {
                        return@supplyAsync EconomyTransactionResponse.NOT_ENOUGH_MONEY
                    }

                    isCreated = false

                    first = AccountRow().apply {
                        this.uuid = uuid
                        this.server = if (currency.global()) "global" else server
                        this.currency = currency.name()
                        this.amount = currency.rookie()
                    }
                }

                type.apply(first, amount)

                if (isCreated) {
                    updateAmountForAccount(currency, uuid, type.toExpression(amount), server)
                } else {
                    accountsDao.create(first)
                }

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
            val account = getAccount(sender, server).join()
            if (account.getBalance(currency.name()) < amount)
                return@supplyAsync EconomyTransactionResponse.NOT_ENOUGH_MONEY

            val senderAccountPair = findOrCreateAccount(sender, currency, server)
            val senderAccount = senderAccountPair.first

            senderAccount.amount -= amount
            if (senderAccountPair.second) {
                updateAmountForAccount(currency, sender, "amount-$amount", server)
            } else {
                accountsDao.create(senderAccount)
            }

            val receiverAccountPair = findOrCreateAccount(receiver, currency, server)
            val receiverAccount = receiverAccountPair.first

            receiverAccount.amount += amount
            if (receiverAccountPair.second) {
                updateAmountForAccount(currency, receiver, "amount+$amount", server)
            } else {
                accountsDao.create(receiverAccount)
            }

            return@supplyAsync EconomyTransactionResponse.SUCCESS
        }
    }

    private fun findOrCreateAccount(uuid: UUID, currency: ICurrency, server: String): Pair<AccountRow, Boolean> {
        var account = findAccount(uuid, currency, server)
        val created = account != null

        if (account == null) {
            account = AccountRow().apply {
                this.uuid = uuid
                this.currency = currency.name()
                this.amount = currency.rookie()
                this.server = if (currency.global()) "global" else server
            }
        }

        return account to created
    }

    private fun updateAmountForAccount(currency: ICurrency, uuid: UUID, expression: String, server: String) {
        val updateBuilder = accountsDao.updateBuilder()

        updateBuilder.setWhere(
            updateBuilder.where()
                .eq("uuid", uuid)
                .and()
                .eq("server", if (currency.global()) "global" else server)
                .and()
                .eq("currency", currency.name())
        )

        updateBuilder.updateColumnValue("amount", expression)
        updateBuilder.update()
    }

    private fun findAccount(uuid: UUID, currency: ICurrency, server: String): AccountRow? {
        val queryBuilder = accountsDao.queryBuilder()
            .where()
            .eq("uuid", uuid)
            .and()
            .eq("currency", currency.name())
            .and()
            .matchServerOrGlobal(server, currency.global())

        val first = queryBuilder.queryForFirst()

        return first
    }

    private fun EconomyTransactionType.apply(row: AccountRow, amount: Double) {
        when (this) {
            EconomyTransactionType.ADD -> {
                row.amount += amount
            }

            EconomyTransactionType.TAKE -> {
                row.amount -= amount
            }

            EconomyTransactionType.SET -> {
                row.amount = amount
            }
        }
    }

    private fun EconomyTransactionType.toExpression(amount: Double): String {
        return when (this) {
            EconomyTransactionType.ADD -> {
                "amount+$amount"
            }

            EconomyTransactionType.SET -> {
                "$amount"
            }

            EconomyTransactionType.TAKE -> {
                "amount-$amount"
            }
        }
    }

}