package me.snipz.economy.menu

import me.snipz.economy.management.CurrenciesManager
import me.snipz.economy.toComponent
import me.snipz.gui.item.FillerButton
import me.snipz.gui.item.GUIButton
import me.snipz.gui.pagination.FillersData
import me.snipz.gui.pagination.PaginatedGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CurrenciesMenu(player: Player, private val currencies: List<CurrenciesManager.Currency>) :
    PaginatedGUI(player, 54, "Валюты".toComponent()) {

    private val previousPageSlots = listOf(9, 18, 27, 36)
    private val nextPageSlots = listOf(17, 26, 35, 44)

    override fun redraw() {

        if (page != 0) {
            addButton(GUIButton().apply {
                this.itemStack = ItemStack(Material.RED_STAINED_GLASS_PANE)
                this.action = {
                    previousPage()
                    redraw()
                }
                this.slots = previousPageSlots
            })
        } else {
            previousPageSlots.forEach {
                removeButton(it)
            }
        }

        if (!isLastPage()) {
            addButton(GUIButton().apply {
                this.itemStack = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
                this.action = {
                    nextPage()
                    redraw()
                }
                this.slots = nextPageSlots
            })
        } else {
            nextPageSlots.forEach {
                removeButton(it)
            }
        }
    }

    override fun prepareFillers(): FillersData {

        val slots = mutableListOf<Int>()

        slots.addAll((11..15))
        slots.addAll((20..24))
        slots.addAll((29..33))
        slots.addAll((38..42))

        val items = mutableListOf<FillerButton>()

        currencies.forEach { currency ->
            items.add(FillerButton().apply {
                val item = ItemStack(Material.BOOK)
                item.editMeta {
                    it.displayName(currency.name.toComponent().color(NamedTextColor.GREEN))
                    it.lore(
                        listOf(
                            Component.text(" "),
                            "&f Вид синхронизации: &a${currency.global.toSyncType()}".toComponent(),
                            "&f Символ: &a${currency.symbol}".toComponent(),
                            Component.text(" ")
                        )
                    )
                }

                this.itemStack = item
                this.action = { }
            })
        }

        return FillersData(items, slots)
    }

    private fun Boolean.toSyncType(): String {
        return if (this) "Глобальная" else "Локальная"
    }
}