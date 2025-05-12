package me.snipz.economy.commands

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.commands.argument.CurrencyArgumentType
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.menu.CurrenciesMenu
import org.bukkit.entity.Player

class CurrenciesCommand(private val plugin: EconomyPlugin) {

    fun register(commands: Commands) {

        commands.register(
            Commands.literal("currencies")
                .requires { it.sender.hasPermission("economy.admin") }
                .executes { ctx ->
                    if (ctx.source.sender !is Player)
                        return@executes 1

                    val player = ctx.source.sender as Player
                    openCurrenciesMenu(player)

                    return@executes 1
                }
                .then(
                    Commands.literal("publish")
                        .executes {
                            val sender = it.source.sender

                            sender.sendMessage(" ")
                            sender.sendMessage("Валюты успешно выгружены в базу данных...")
                            sender.sendMessage("Теперь они синхронизированы между серверами.")
                            sender.sendMessage(" ")

                            CurrenciesManager.publish()

                            return@executes 1
                        })
                .build()
        )

        commands.register(
            Commands.literal("currency")
                .requires { it.sender.hasPermission("economy.admin") }
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument("currency", StringArgumentType.word())
                                .executes { ctx ->

                                    val name = StringArgumentType.getString(ctx, "currency")
                                    if (CurrenciesManager.isCurrency(name)) {
                                        ctx.source.sender.sendMessage("Валюта $name уже существует.")
                                        return@executes 1
                                    }

                                    val currency = CurrenciesManager.Currency(name, false, "")
                                    CurrenciesManager.add(currency.name, currency)

                                    ctx.source.sender.sendMessage("Валюта $name сохранена.")

                                    return@executes 1
                                })
                )
                .then(
                    Commands.literal("delete")
                        .then(
                            Commands.argument("currency", CurrencyArgumentType(plugin))
                                .executes { ctx ->

                                    val currency = ctx.getArgument("currency", CurrenciesManager.Currency::class.java)
                                    val sender = ctx.source.sender

                                    CurrenciesManager.delete(currency.name)
                                    sender.sendMessage("Валюта ${currency.name} удалена.")

                                    return@executes 1
                                }
                        )
                )
                .then(
                    Commands.literal("symbol")
                        .then(
                            Commands.argument("currency", CurrencyArgumentType(plugin))
                                .then(
                                    Commands.argument("symbol", StringArgumentType.greedyString())
                                        .executes { ctx ->

                                            val sender = ctx.source.sender
                                            val currency =
                                                ctx.getArgument("currency", CurrenciesManager.Currency::class.java)
                                            val symbol = StringArgumentType.getString(ctx, "symbol").split(" ")[0]

                                            currency.symbol = symbol

                                            sender.sendMessage("Символ валюты ${currency.name} успешно изменён на $symbol")

                                            return@executes 1
                                        })
                        )
                )
                .then(
                    Commands.literal("global")
                        .then(
                            Commands.argument("currency", CurrencyArgumentType(plugin))
                                .then(
                                    Commands.argument("global", BoolArgumentType.bool())
                                        .executes { ctx ->

                                            val sender = ctx.source.sender
                                            val currency =
                                                ctx.getArgument("currency", CurrenciesManager.Currency::class.java)

                                            val global = BoolArgumentType.getBool(ctx, "global")

                                            currency.global = global

                                            if (global) {
                                                sender.sendMessage("Валюта ${currency.name} теперь синхронизирована между всеми серверами.")
                                            } else {
                                                sender.sendMessage("Валюта ${currency.name} теперь разделена на всех серверах.")
                                            }

                                            return@executes 1
                                        })
                        )
                )
                .build()
        )

    }

    private fun openCurrenciesMenu(player: Player) {
        CurrenciesMenu(player, CurrenciesManager.get().toList()).open()
    }

}