package me.snipz.economy.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.economy.EconomyTransactionType
import me.snipz.api.sendLocaleMessage
import me.snipz.api.toComponent
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.commands.argument.CurrencyArgumentType
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import net.kyori.adventure.text.TextReplacementConfig
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

                    EconomyManager.getUnloadedAccount(sender.uniqueId).thenAccept { account ->
                        plugin.locale.getMessage("your-balance")?.let { msg ->
                            sender.sendMessage(
                                msg.toComponent().replaceText(
                                    TextReplacementConfig.builder().matchLiteral("{balance}")
                                        .replacement(account.getBalanceString())
                                        .build()
                                )
                            )
                        }
                    }

                    return@executes 1
                }
                .then(
                    Commands.argument("target", StringArgumentType.word())
                        .requires { it.sender.hasPermission("economy.command.balance.other") }
                        .executes {

                            val target = StringArgumentType.getString(it, "target")
                            val targetId = Bukkit.getOfflinePlayer(target).uniqueId

                            EconomyManager.getUnloadedAccount(targetId).thenAccept { account ->
                                plugin.locale.getMessage("other-balance")?.let { msg ->
                                    it.source.sender.sendMessage(
                                        msg.toComponent().replaceText(
                                            TextReplacementConfig.builder().matchLiteral("{target}").replacement(target)
                                                .build()
                                        )
                                            .replaceText(
                                                TextReplacementConfig.builder().matchLiteral("{balance}")
                                                    .replacement(account.getBalanceString()).build()
                                            )
                                    )
                                }
                            }

                            return@executes 1
                        }
                )
                .build()
        )

    }

    private fun execute(ctx: CommandContext<CommandSourceStack>, type: EconomyTransactionType) {
        val sender = ctx.source.sender
        val targetName = StringArgumentType.getString(ctx, "name")
        val target = Bukkit.getOfflinePlayer(targetName).uniqueId

        val amount = EconomyManager.formatDouble(DoubleArgumentType.getDouble(ctx, "amount"))

        val currency = ctx.getArgument(
            "currency",
            CurrenciesManager.Currency::class.java
        )

        EconomyManager.tryTransaction(
            target,
            currency,
            amount,
            type
        ).thenAccept {
            when (it) {
                EconomyTransactionResponse.SUCCESS -> {
                    plugin.locale.getMessage("transaction-success")?.let { msg ->
                        EconomyManager.getUnloadedAccount(target).thenAccept { account ->
                            sender.sendLocaleMessage(
                                msg.replace("{target}", targetName)
                                    .replace(
                                        "{balance}",
                                        account.getBalance(currency.name).toString() + currency.symbol
                                    )
                            )
                        }
                    }
                }

                EconomyTransactionResponse.EXCEPTION -> {
                    plugin.locale.getMessage("exception")?.let {
                        sender.sendLocaleMessage(it)
                    }
                }

                EconomyTransactionResponse.NOT_ENOUGH_MONEY -> {
                    sender.sendMessage("На счету недостаточно средств для этой транзакции.")
                    plugin.locale.getMessage("transaction-fail-no-money")?.let { msg ->
                        sender.sendLocaleMessage(msg.replace("{target}", targetName))
                    }
                }

                else -> {

                }
            }

        }
    }

}