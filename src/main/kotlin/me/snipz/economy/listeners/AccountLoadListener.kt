package me.snipz.economy.listeners

import me.snipz.api.listener.QuickListener
import me.snipz.economy.management.EconomyManager
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerJoinEvent

class AccountLoadListener(private val manager: EconomyManager) : QuickListener() {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        manager.asyncLoadOnJoin(event.player.uniqueId)
    }

}