package me.snipz.economy.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import me.snipz.economy.EconomyLocale
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.api.EconomyTransactionResponse
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import me.snipz.economy.`object`.Account.Companion.formatDouble
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PayCommand(private val plugin: EconomyPlugin) {

    fun register(commands: Commands) {

        val currency = plugin.config.getString("payment-currency") ?: return
        if (!CurrenciesManager.isCurrency(currency))
            return

        commands.register(
            Commands.literal("pay")
                .requires { it.sender is Player && it.sender.hasPermission("economy.command.pay") }
                .then(
                    Commands.argument("target", StringArgumentType.word())
                        .then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0.1))
                                .executes { ctx ->

                                    val player = ctx.source.sender as Player
                                    val targetName = StringArgumentType.getString(ctx, "target")

                                    if (targetName == player.name) {
                                        plugin.msgConfig.getMessage(EconomyLocale.CANT_PAY_YOURSELF).send(player)
                                        return@executes 1
                                    }

                                    val targetUUID = Bukkit.getOfflinePlayer(targetName).uniqueId
                                    val amount =
                                        formatDouble(DoubleArgumentType.getDouble(ctx, "amount"))

                                    val amountString = "$amount${CurrenciesManager.getCurrency(currency)!!.symbol}"

                                    val targetPlayer = Bukkit.getPlayer(targetUUID)

                                    EconomyManager.tryTransfer(
                                        player.uniqueId,
                                        targetUUID,
                                        CurrenciesManager.getCurrency(currency)!!,
                                        amount
                                    ).thenAccept { result ->
                                        when (result) {
                                            EconomyTransactionResponse.SUCCESS -> {
                                                plugin.msgConfig.getMessage(
                                                    EconomyLocale.PAYMENT_SENT,
                                                ).send(player, "{amount}" to amountString, "{target}" to targetName)

                                                if (targetPlayer != null) {
                                                    plugin.msgConfig.getMessage(
                                                        EconomyLocale.PAYMENT_RECEIVED,
                                                    ).send(
                                                        targetPlayer,
                                                        "{sender}" to player.name,
                                                        "{amount}" to amountString
                                                    )
                                                }
                                            }

                                            EconomyTransactionResponse.NOTHING_CHANGED -> {
                                                plugin.msgConfig.getMessage(
                                                    EconomyLocale.PAYMENT_NOT_ENOUGH_MONEY
                                                ).send(player)
                                            }

                                            else -> {
                                                plugin.msgConfig.getMessage(EconomyLocale.EXCEPTION)
                                                    .send(player)
                                            }
                                        }
                                    }

                                    return@executes 1
                                }
                        )
                )
                .build(),
            listOf("sendmoney")
        )
    }

}