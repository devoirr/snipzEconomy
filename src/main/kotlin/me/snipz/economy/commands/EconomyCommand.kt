package me.snipz.economy.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.snipz.economy.EconomyLocale
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.api.EconomyTransactionType
import me.snipz.economy.commands.argument.CurrencyArgumentType
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import me.snipz.economy.`object`.Account.Companion.formatDouble
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class EconomyCommand(
    private val plugin: EconomyPlugin
) {

    fun register(commands: Commands) {

        commands.register(
            Commands.literal("economy")
                .requires { it.sender.hasPermission("economy.admin") }
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .then(
                                    Commands.argument("currency", CurrencyArgumentType(plugin))
                                        .then(
                                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.1))
                                                .executes { ctx ->
                                                    execute(ctx, type = EconomyTransactionType.ADD)
                                                    return@executes 1
                                                }
                                        )
                                )
                        )
                )
                .then(
                    Commands.literal("take")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .then(
                                    Commands.argument("currency", CurrencyArgumentType(plugin))
                                        .then(
                                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.1))
                                                .executes {
                                                    execute(it, type = EconomyTransactionType.TAKE)
                                                    return@executes 1
                                                })
                                )
                        )
                )
                .then(
                    Commands.literal("set")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .then(
                                    Commands.argument("currency", CurrencyArgumentType(plugin))
                                        .then(
                                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
                                                .executes {
                                                    execute(it, type = EconomyTransactionType.SET)
                                                    return@executes 1
                                                })
                                )
                        )
                )
                .build(),
            listOf("eco", "money")
        )

        commands.register(
            Commands.literal("balance")
                .requires { it.sender.hasPermission("economy.command.balance") }
                .executes {
                    val sender = it.source.sender
                    if (sender !is Player) {
                        sender.sendMessage("Используйте /balance <Игрок>")
                        return@executes 1
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val account = EconomyManager.getAccount(sender.uniqueId)

                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.msgConfig.getMessage(EconomyLocale.YOUR_BALANCE)
                                .send(sender, "{balance}" to account.getBalanceString())
                        })
                    }
                    return@executes 1
                }
                .then(
                    Commands.argument("target", StringArgumentType.word())
                        .requires { it.sender.hasPermission("economy.command.balance.other") }
                        .executes {

                            val target = StringArgumentType.getString(it, "target")
                            val targetId = Bukkit.getOfflinePlayer(target).uniqueId

                            CoroutineScope(Dispatchers.IO).launch {
                                val account = EconomyManager.getAccount(targetId)
                                Bukkit.getScheduler().runTask(plugin, Runnable {
                                    plugin.msgConfig.getMessage(EconomyLocale.OTHER_BALANCE)
                                        .send(
                                            it.source.sender,
                                            "{balance}" to account.getBalanceString(),
                                            "{target}" to target
                                        )
                                })
                            }

                            return@executes 1
                        }
                )
                .build(),
            listOf("bal")
        )

    }

    private fun execute(ctx: CommandContext<CommandSourceStack>, type: EconomyTransactionType) {
        val sender = ctx.source.sender
        val targetName = StringArgumentType.getString(ctx, "name")
        val target = Bukkit.getOfflinePlayer(targetName).uniqueId

        val amount = formatDouble(DoubleArgumentType.getDouble(ctx, "amount"))

        val currency = ctx.getArgument(
            "currency",
            CurrenciesManager.Currency::class.java
        )

        CoroutineScope(Dispatchers.IO).launch {
            val response = EconomyManager.tryTransaction(target, currency, amount, type)
            when (response) {
                EconomyTransactionResponse.SUCCESS -> {
                    val account = EconomyManager.getAccount(target)
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        plugin.msgConfig.getMessage(EconomyLocale.TRANSACTION_SUCCESS)
                            .send(sender, "{balance}" to account.getBalanceString())
                    })
                }

                EconomyTransactionResponse.EXCEPTION -> {
                    plugin.msgConfig.getMessage(EconomyLocale.EXCEPTION).send(sender)
                }

                EconomyTransactionResponse.NOTHING_CHANGED -> {

                    val account = EconomyManager.getAccount(target)

                    if (type == EconomyTransactionType.TAKE) {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.msgConfig.getMessage(EconomyLocale.ACCOUNT_NOT_FOUND_OR_NO_MONEY)
                                .send(sender, "{balance}" to account.getBalanceString())
                        })
                    } else {
                        Bukkit.getScheduler().runTask(plugin, Runnable {
                            plugin.msgConfig.getMessage(EconomyLocale.ACCOUNT_NOT_FOUND)
                                .send(sender, "{balance}" to account.getBalanceString())
                        })
                    }

                }

                else -> {
                }
            }
        }
    }

}