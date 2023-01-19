package me.fzzyhmstrs.amethyst_core.modifier_util

import me.fzzyhmstrs.amethyst_core.AC
import me.fzzyhmstrs.amethyst_core.nbt_util.Nbt
import me.fzzyhmstrs.amethyst_core.nbt_util.NbtKeys
import me.fzzyhmstrs.amethyst_core.registry.ModifierRegistry
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier

object ModifierHelper: AbstractModifierHelper<AugmentModifier>() {

    override val fallbackData: AbstractModifier<AugmentModifier>.CompiledModifiers = ModifierDefaults.BLANK_COMPILED_DATA

    fun addModifierForREI(modifier: Identifier, stack: ItemStack){
        val nbt = stack.orCreateNbt
        Nbt.makeItemStackId(stack)
        addModifierToNbt(modifier, nbt)
    }

    fun isInTag(id: Identifier,tag: TagKey<Enchantment>): Boolean{
        val augment = Registries.ENCHANTMENT.get(id)?:return false
        val opt = Registries.ENCHANTMENT.getEntry(Registries.ENCHANTMENT.getRawId(augment))
        var bl = false
        opt.ifPresent { entry -> bl = entry.isIn(tag) }
        return bl
    }

    fun createAugmentTag(path: String): TagKey<Enchantment> {
        return TagKey.of(RegistryKeys.ENCHANTMENT, Identifier(AC.MOD_ID,path))
    }

    override fun gatherActiveModifiers(stack: ItemStack){
        val nbt = stack.nbt
        if (nbt != null) {
            val id = Nbt.getItemStackId(nbt)
            if (!nbt.contains(NbtKeys.ACTIVE_ENCHANT.str())) return
            val activeEnchant = Identifier(Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt))
            setModifiersById(
                id,
                gatherActiveAbstractModifiers(stack, activeEnchant, ModifierDefaults.BLANK_AUG_MOD.compiler())
            )
        }
    }

    override fun getTranslationKeyFromIdentifier(id: Identifier): String {
        return "scepter.modifier.${id}"
    }
    
    override fun getDescTranslationKeyFromIdentifier(id: Identifier): String {
        return "scepter.modifier.${id}.desc"
    }

    override fun getModifierByType(id: Identifier): AugmentModifier? {
        return ModifierRegistry.getByType<AugmentModifier>(id)
    }
}
