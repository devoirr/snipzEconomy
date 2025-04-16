package me.snipz.economy.commands.argument

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import me.snipz.api.toComponent
import me.snipz.economy.EconomyPlugin
import me.snipz.economy.management.CurrenciesManager
import java.util.concurrent.CompletableFuture

class CurrencyArgumentType(private val plugin: EconomyPlugin) :
    CustomArgumentType.Converted<CurrenciesManager.Currency, String> {

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun convert(p0: String): CurrenciesManager.Currency {
        val currency = CurrenciesManager.getCurrency(p0)

        if (currency == null) {
            val text = plugin.locale.getMessage("currency-not-found")?.replace("{name}", p0) ?: "Валюта $p0 не найдена."

            val message = MessageComponentSerializer.message().serialize(text.toComponent())
            throw CommandSyntaxException(SimpleCommandExceptionType(message), message)
        } else {
            return currency
        }
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        for (currency in CurrenciesManager.get()) {
            if (currency.name.lowercase().startsWith(builder.remainingLowerCase)) {
                builder.suggest(currency.name)
            }
        }

        return builder.buildFuture()
    }
}