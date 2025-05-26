package me.snipz.economy.hook

import me.snipz.economy.management.EconomyManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

class UsersListener : Listener {

    @EventHandler
    fun onLogIn(event: AsyncPlayerPreLoginEvent) {
        EconomyManager.loadAccount(event.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        EconomyManager.unloadAccount(event.player.uniqueId)
    }

}