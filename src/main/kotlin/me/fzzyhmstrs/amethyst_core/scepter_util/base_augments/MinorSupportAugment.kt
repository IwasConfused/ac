package me.fzzyhmstrs.amethyst_core.scepter_util.base_augments

import me.fzzyhmstrs.amethyst_core.modifier_util.AugmentEffect
import me.fzzyhmstrs.amethyst_core.raycaster_util.RaycasterUtil
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Hand
import net.minecraft.world.World

abstract class MinorSupportAugment(tier: Int, maxLvl: Int, vararg slot: EquipmentSlot): ScepterAugment(tier,maxLvl,EnchantmentTarget.WEAPON, *slot) {

    override val baseEffect: AugmentEffect
        get() = super.baseEffect.withRange(6.0,0.0, 0.0)

    override fun applyTasks(world: World, user: LivingEntity, hand: Hand, level: Int, effects: AugmentEffect): Boolean {
        val target = RaycasterUtil.raycastEntity(distance = effects.range(level),user)
        return supportEffect(world, target, user, level, effects)
    }

    open fun supportEffect(world: World, target: Entity?, user: LivingEntity, level: Int, effects: AugmentEffect): Boolean {
        return false
    }
}