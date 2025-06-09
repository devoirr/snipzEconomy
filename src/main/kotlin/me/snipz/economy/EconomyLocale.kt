package me.snipz.economy

import me.snipz.api.locale.objects.LocaleEnum
import me.snipz.api.locale.objects.Message


enum class EconomyLocale(private vararg val values: String) : LocaleEnum {

    TRANSACTION_SUCCESS("", "Транзакция прошла успешно!", "Текущий баланс игрока: <green>{balance}</green>", ""),
    EXCEPTION("", "<red>Произошла непредвиденная ошибка.", "<red>Администрация уведомлена и уже разбирается.", ""),
    ACCOUNT_NOT_FOUND("<red>Аккаунт игрока не найден.</red>"),
    ACCOUNT_NOT_FOUND_OR_NO_MONEY("<red>Аккаунт не найден, либо на счету недостаточно средств.</red>"),

    YOUR_BALANCE("", "Ваш баланс: <green>{balance}", "Способы заработка денег - <green>/earn", ""),
    OTHER_BALANCE("Баланс игрока <green>{target}</green> на сервере - <green>{balance}</green>"),

    CANT_PAY_YOURSELF("Вы не можете заплатить сами себе!"),
    PAYMENT_SENT("", "Оплата прошла успешно! ", "Вы перевели <green>{amount}</green> игроку <green>{target}", ""),
    PAYMENT_RECEIVED("", "Новый перевод!", "Игрок <green>{sender}</green> перевёл вам <green>{amount}", ""),
    PAYMENT_NOT_ENOUGH_MONEY("", "У вас недостаточно средств.", "Способы заработка денег - <green>/earn", ""),

    CURRENCY_NOT_FOUND("Валюта не найдена.")
    ;

    override fun getMessage(): Message {
        return Message(values.toList())
    }
}