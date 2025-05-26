package me.snipz.economy

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import me.snipz.database.impl.H2Database
import me.snipz.database.impl.MySQLDatabase
import me.snipz.eco.EconomyService
import me.snipz.economy.database.EconomyDatabase
import me.snipz.economy.hook.EconomyPlaceholders
import me.snipz.economy.hook.EconomyServiceHook
import me.snipz.economy.hook.EconomyVaultService
import me.snipz.economy.hook.UsersListener
import me.snipz.economy.management.EconomyManager
import me.snipz.locales.LocalesRegistry
import me.snipz.locales.objects.LocaleConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.milkbowl.vault.economy.Economy
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.*

class EconomyPlugin : JavaPlugin() {

    private lateinit var database: EconomyDatabase

    lateinit var msgConfig: LocaleConfig<EconomyLocale>

    /**
     * Локальные валюты записываются отдельно для всех серверов с их serverId.
     */
    lateinit var serverId: String

    override fun onEnable() {
        this.saveDefaultConfig()

        this.readOrCreateServerId()

        this.msgConfig = LocalesRegistry.buildLocaleConfig<EconomyLocale>(this)
        this.msgConfig.reload()

        this.initDatabase()

        EconomyManager.onEnable(this, database)
        EconomyPlaceholders().register()

        this.server.pluginManager.registerEvents(UsersListener(), this)

        server.servicesManager.register(
            EconomyService::class.java,
            EconomyServiceHook(database, serverId),
            this,
            ServicePriority.Normal
        )

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(
                Commands.literal("reloadeconomy")
                    .requires { it.sender.hasPermission("economy.admin") }
                    .executes { context ->

                        this.msgConfig.reload()
                        context.source.sender.sendMessage(
                            Component.text(
                                "Сообщения экономики перезагружены.",
                                NamedTextColor.GREEN
                            )
                        )

                        return@executes 1
                    }
                    .build()
            )
        }

        this.hookIntoVault()
    }

    private fun initDatabase() {
        val type = config.getString("database.type")?.lowercase() ?: "h2"

        if (type == "h2") {
            val fileName = (config.getString("database.file-name") ?: "database") + ".db"
            this.database = EconomyDatabase(
                H2Database(File(dataFolder, fileName).absolutePath),
            )
        } else if (type == "mysql") {
            val host = config.getString("database.host") ?: "localhost"
            val port = config.getInt("database.port")
            val db = config.getString("database.database") ?: "economy"
            val username = config.getString("database.username") ?: "root"
            val password = config.getString("database.password") ?: "password"

            this.database = EconomyDatabase(
                MySQLDatabase(host, port, db, username, password, 8),
            )
        }
    }

    private fun hookIntoVault() {
        if (config.getKeys(false).contains("vault-currency")) {
            val vaultCurrency = config.getString("vault-currency") ?: return

            server.servicesManager.register(
                Economy::class.java,
                EconomyVaultService(vaultCurrency),
                this,
                ServicePriority.Highest
            )
        }
    }

    private fun readOrCreateServerId() {
        if (config.getKeys(false).contains("server-id")) {
            serverId = config.getString("server-id")!!
        } else {
            serverId = UUID.randomUUID().toString().split("-")[0]
            config.set("server-id", serverId)

            saveConfig()
        }
    }

}

fun String.toComponent() = LegacyComponentSerializer.legacyAmpersand().deserialize(this)