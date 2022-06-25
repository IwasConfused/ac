package me.fzzyhmstrs.amethyst_core.item_util

import net.minecraft.item.ItemStack
import kotlin.math.max
import kotlin.math.min

interface ManaItem {

    //interface used for type comparison
    fun healDamage(amount: Int, stack: ItemStack): Int{
        val healedAmount = min(amount,stack.damage)
        stack.damage = max(0,stack.damage - amount)
        return healedAmount
    }

}