package me.fzzyhmstrs.amethyst_core.item_util

import me.fzzyhmstrs.amethyst_core.AC
import net.minecraft.client.item.TooltipContext
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.world.World

open class CustomFlavorItem(settings: Settings, flavor: String, glint: Boolean, nameSpace: String = AC.MOD_ID) : Item(settings) {

    private val ns: String = nameSpace
    private val ttn: String = flavor
    private val itemGlint: Boolean = glint

    override fun appendTooltip(stack: ItemStack, world: World?, tooltip: MutableList<Text>, context: TooltipContext) {
        super.appendTooltip(stack, world, tooltip, context)
        tooltip.add(TranslatableText("item.$ns.$ttn.tooltip1").formatted(Formatting.WHITE, Formatting.ITALIC))
    }

    override fun hasGlint(stack: ItemStack): Boolean {
        return if (itemGlint) {
            true
        } else {
            super.hasGlint(stack)
        }
    }
}