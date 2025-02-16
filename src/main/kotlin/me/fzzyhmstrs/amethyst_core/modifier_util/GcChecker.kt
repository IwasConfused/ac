package me.fzzyhmstrs.amethyst_core.modifier_util

import net.fabricmc.loader.api.FabricLoader

object GcChecker {

    val gearCoreLoaded: Boolean by lazy{
        FabricLoader.getInstance().isModLoaded("gear_core")
    }

    fun registerProcessor(){
        if (gearCoreLoaded){
            GcCompat.registerAugmentModifierProcessor()
        }
    }

}