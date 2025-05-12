package me.snipz.economy.hook

import me.snipz.economy.database.EconomyDatabase
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import java.util.*

class UsersListener(private val database: EconomyDatabase, private val server: String) : Listener {

    private val uuids = mutableListOf<UUID>()

    @EventHandler
    fun onLogIn(event: AsyncPlayerPreLoginEvent) {
        if (event.uniqueId in uuids)
            return

        database.createAccount(event.uniqueId, server)
        uuids.add(event.uniqueId)
    }

}