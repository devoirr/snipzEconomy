package me.snipz.economy.commands

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import io.papermc.paper.command.brigadier.Commands
import me.snipz.api.economy.EconomyTransactionResponse
import me.snipz.api.sendLocaleMessage
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
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
                                        plugin.locale.getMessage("payment-cant-pay-self")?.let { msg ->
                                            player.sendLocaleMessage(msg)
                                        }
                                        return@executes 1
                                    }

                                    val targetUUID = Bukkit.getOfflinePlayer(targetName).uniqueId
                                    val amount =
                                        EconomyManager.formatDouble(DoubleArgumentType.getDouble(ctx, "amount"))

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
                                                plugin.locale.getMessage("payment-sent")?.let { msg ->
                                                    player.sendLocaleMessage(
                                                        msg.replace("{target}", targetName)
                                                            .replace("{amount}", amountString)
                                                    )
                                                }

                                                if (targetPlayer != null) {
                                                    plugin.locale.getMessage("payment-received")?.let { msg ->
                                                        player.sendLocaleMessage(
                                                            msg.replace("{sender}", player.name)
                                                                .replace("{amount}", amountString)
                                                        )
                                                    }
                                                }
                                            }

                                            EconomyTransactionResponse.NOT_ENOUGH_MONEY -> {
                                                plugin.locale.getMessage("payment-not-enough-money")?.let { msg ->
                                                    player.sendLocaleMessage(msg)
                                                }
                                            }

                                            else -> {
                                                plugin.locale.getMessage("exception")?.let {
                                                    player.sendLocaleMessage(it)
                                                }
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