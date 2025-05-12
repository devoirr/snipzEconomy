package me.snipz.economy.database

class SQLiteQueries {

    companion object {
        const val UPDATE_OR_AMOUNT_EXPRESSION =
            "insert into accounts values (?,?,?,?) on conflict (player, server, currency) do update set amount = {expression}"
    }

}

class MySQLQueries {
    companion object {
        const val UPDATE_OR_AMOUNT_EXPRESSION =
            "insert into accounts values(?,?,?,?) on duplicate key update amount={expression}"
    }
}