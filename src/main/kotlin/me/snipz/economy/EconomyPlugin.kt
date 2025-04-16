package me.snipz.economy

import me.snipz.api.database.DatabaseInfo
import me.snipz.api.locale.Messages
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.hook.EconomyPlaceholders
import me.snipz.economy.hook.EconomyVaultService
import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.management.EconomyManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class EconomyPlugin : JavaPlugin() {

    private lateinit var database: EconomyDatabase
    lateinit var locale: Messages
    lateinit var serverId: String

    override fun onEnable() {
        this.saveDefaultConfig()
        this.saveResource("messages.json", false)

        if (config.getKeys(false).contains("server-id")) {
            serverId = config.getString("server-id")!!
        } else {
            serverId = UUID.randomUUID().toString().split("-")[0]
            config.set("server-id", serverId)

            saveConfig()
        }

        this.locale = Messages.load(File(dataFolder, "messages.json"))!!
        this.database = EconomyDatabase(DatabaseInfo.parse(config.getConfigurationSection("database")!!, this))

        EconomyManager.onEnable(this, database)

        EconomyPlaceholders().register()

        if (config.getKeys(false).contains("vault-currency")) {
            val vaultCurrency = config.getString("vault-currency") ?: return

            if (!CurrenciesManager.isCurrency(vaultCurrency))
                return

            server.servicesManager.register(
                Economy::class.java,
                EconomyVaultService(vaultCurrency),
                this,
                ServicePriority.High
            )
        }
    }

}