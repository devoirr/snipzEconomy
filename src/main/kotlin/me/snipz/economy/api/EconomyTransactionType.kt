package me.snipz.economy.api

enum class EconomyTransactionType {

    ADD,
    SET,
    TAKE;

    fun toExpression(amount: Double): String {
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

    fun apply(default: Double, amount: Double): Double {
        return when (this) {
            ADD -> {
                default + amount
            }

            SET -> {
                amount
            }

            TAKE -> {
                default - amount
            }
        }
    }
}

enum class EconomyTransactionResponse {

    SUCCESS,
    EXCEPTION,
    AMOUNT_NEGATIVE,
    NOTHING_CHANGED

}