package me.fzzyhmstrs.amethyst_core.scepter_util.base_augments

import me.fzzyhmstrs.amethyst_core.raycaster_util.RaycasterUtil
import me.fzzyhmstrs.amethyst_core.scepter_util.AugmentEffect
import net.minecraft.enchantment.EnchantmentTarget
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.BoatEntity
import net.minecraft.predicate.entity.EntityPredicates
import net.minecraft.sound.SoundCategory
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World


abstract class SummonEntityAugment(tier: Int, maxLvl: Int, vararg slot: EquipmentSlot): ScepterAugment(tier,maxLvl,EnchantmentTarget.WEAPON, *slot) {

    override val baseEffect: AugmentEffect
        get() = AugmentEffect().withRange(3.0,0.0,0.0)

    override fun applyTasks(
        world: World,
        user: LivingEntity,
        hand: Hand,
        level: Int,
        effects: AugmentEffect
    ): Boolean {
        if (user !is PlayerEntity) return false
        val hit = RaycasterUtil.raycastHit(
            distance = effects.range(level),
            user,
            includeFluids = true
        ) ?: return false
        if (hit.type != HitResult.Type.BLOCK) return false
        return placeEntity(world, user, hit, level, effects)
    }

    open fun placeEntity(world: World, user: PlayerEntity, hit: HitResult, level: Int, effects: AugmentEffect): Boolean{
        val vec3d2: Vec3d
        val vec3d: Vec3d = user.getRotationVec(1.0f)
        val list: List<Entity> = world.getOtherEntities(
            user,
            user.boundingBox.stretch(vec3d.multiply(5.0)).expand(1.0),
            EntityPredicates.EXCEPT_SPECTATOR.and { obj: Entity -> obj.collides() }
        )
        if (list.isNotEmpty()) {
            vec3d2 = user.eyePos
            for (entity in list) {
                val box = entity.boundingBox.expand(entity.targetingMargin.toDouble())
                if (!box.contains(vec3d2)) continue
                return false
            }
        }
        val boat = BoatEntity(world, hit.pos.x, hit.pos.y, hit.pos.z)
        boat.boatType = BoatEntity.Type.OAK
        boat.yaw = user.yaw
        if (!world.isSpaceEmpty(boat, boat.boundingBox)) {
            return false
        }
        world.spawnEntity(boat)
        world.playSound(null,user.blockPos,soundEvent(),SoundCategory.PLAYERS,1.0F,1.0F)
        return true
    }

}