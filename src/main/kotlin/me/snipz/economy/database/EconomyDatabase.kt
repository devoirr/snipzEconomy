package me.snipz.economy.database

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.snipz.api.database.AbstractDatabase
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.api.ICurrency
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.`object`.Account
import java.util.*


class EconomyDatabase(
    private val database: AbstractDatabase,
) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            database.update(
                "create table if not exists currencies(name varchar(32) primary key, global bool, symbol varchar(16));"
            )

            database.update(
                "create table if not exists accounts (player varchar(36), server varchar(36), currency varchar(32), amount double, primary key (player, server, currency));"
            )
        }
    }

    suspend fun getSuspendAccount(uuid: UUID, server: String): Account {
        val balances = mutableMapOf<String, Double>()
        database.query(
            "select currency, amount from accounts where player=? and (server=? or server='global')",
            { resultSet ->
                while (resultSet.next()) {
                    balances[resultSet.getString("currency")] = resultSet.getDouble("amount")
                }
            },
            uuid.toString(),
            server
        )

        CurrenciesManager.get().forEach { currency ->
            if (!balances.containsKey(currency.name)) {
                balances[currency.name] = 0.0
            }
        }

        val account = Account(uuid, balances)
        return account
    }

    suspend fun addMoney(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        server: String
    ): EconomyTransactionResponse {
        return positiveTransaction(uuid, currency, amount, server, "amount + $amount")
    }

    suspend fun setMoney(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        server: String
    ): EconomyTransactionResponse {
        return positiveTransaction(uuid, currency, amount, server, "$amount")
    }

    suspend fun takeMoney(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        server: String
    ): EconomyTransactionResponse {
        if (amount < 0)
            return EconomyTransactionResponse.AMOUNT_NEGATIVE

        val server = if (currency.global()) "global" else server

        val updated =
            database.update(
                "update accounts set amount = amount - ? where amount >= ? and player = ? and server = ? and currency = ?",
                amount, amount, uuid.toString(), server, currency.name()
            )

        if (updated == 0)
            return EconomyTransactionResponse.NOTHING_CHANGED

        return EconomyTransactionResponse.SUCCESS
    }

    suspend fun saveAccount(account: Account, server: String) {
        for (currency in CurrenciesManager.get()) {
            database.update(
                "insert into accounts (player, server, currency, amount) values (?,?,?,?) on duplicate key update amount = ?;",
                account.uuid.toString(),
                if (currency.global) "global" else server,
                currency.name,
                account.getBalance(currency.name),
                account.getBalance(currency.name)
            )
        }
    }

    private suspend fun positiveTransaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        server: String,
        expression: String
    ): EconomyTransactionResponse {
        if (amount < 0)
            return EconomyTransactionResponse.AMOUNT_NEGATIVE

        val updated =
            database.update(
                "insert into accounts (player, server, currency, amount) values (?, ?, ?, ?) on duplicate key update amount = $expression;",
                uuid.toString(), server, currency.name(), amount
            )

        if (updated == 0)
            return EconomyTransactionResponse.NOTHING_CHANGED

        return EconomyTransactionResponse.SUCCESS
    }

    suspend fun tryTransaction(
        uuid: UUID,
        currency: ICurrency,
        amount: Double,
        type: EconomyTransactionType,
        server: String
    ): EconomyTransactionResponse {
        return when (type) {
            EconomyTransactionType.ADD -> {
                addMoney(uuid, currency, amount, server)
            }

            EconomyTransactionType.TAKE -> {
                takeMoney(uuid, currency, amount, server)
            }

            EconomyTransactionType.SET -> {
                setMoney(uuid, currency, amount, server)
            }
        }
    }

    suspend fun transfer(
        sender: UUID,
        receiver: UUID,
        amount: Double,
        currency: ICurrency,
        server: String
    ): EconomyTransactionResponse {
        if (amount < 0) {
            return EconomyTransactionResponse.AMOUNT_NEGATIVE
        }

        val updated =
            database.update(
                "update accounts set amount=amount-$amount where amount >= $amount and currency=? and player = ? and (server = 'global' or server=?);",
                currency.name(),
                sender.toString(),
                server
            )

        if (updated == 0) {
            return EconomyTransactionResponse.NOTHING_CHANGED
        }

        database.update(
            "update accounts set amount=amount+$amount where currency=? and player = ? and (server = 'global' or server=?);",
            currency.name(),
            receiver.toString(),
            server
        )

        return EconomyTransactionResponse.SUCCESS
    }

}