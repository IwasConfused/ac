package me.fzzyhmstrs.amethyst_core.misc_util

import net.fabricmc.fabric.api.loot.v1.FabricLootSupplierBuilder
import net.minecraft.util.Identifier

interface AbstractModLoot {

    fun lootBuilder(id: Identifier, table: FabricLootSupplierBuilder): Boolean

}