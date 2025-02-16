package me.fzzyhmstrs.amethyst_core

import me.fzzyhmstrs.amethyst_core.modifier_util.GcChecker
import me.fzzyhmstrs.amethyst_core.registry.*
import me.fzzyhmstrs.amethyst_core.scepter_util.ScepterHelper
import me.fzzyhmstrs.amethyst_core.scepter_util.augments.PlaceItemAugment
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import net.minecraft.util.Identifier
import kotlin.random.Random


object AC: ModInitializer {
    const val MOD_ID = "amethyst_core"
    val acRandom = Random(System.currentTimeMillis())
    val fallbackId = Identifier("vanishing_curse")

    override fun onInitialize() {
        RegisterBaseEntity.registerAll()
        ModifierRegistry.registerAll()
        GcChecker.registerProcessor()
        ScepterHelper.registerServer()
    }
}

object ACC: ClientModInitializer {
    val acRandom = Random(System.currentTimeMillis())

    override fun onInitializeClient() {
        RegisterBaseRenderer.registerAll()
        PlaceItemAugment.registerClient()
    }
}