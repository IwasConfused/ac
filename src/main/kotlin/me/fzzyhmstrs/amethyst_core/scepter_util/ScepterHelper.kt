@file:Suppress("REDUNDANT_ELSE_IN_WHEN")

package me.fzzyhmstrs.amethyst_core.scepter_util

import me.fzzyhmstrs.amethyst_core.AC
import me.fzzyhmstrs.amethyst_core.item_util.AbstractScepterItem
import me.fzzyhmstrs.amethyst_core.item_util.AugmentScepterItem
import me.fzzyhmstrs.amethyst_core.mana_util.ManaItem
import me.fzzyhmstrs.amethyst_core.modifier_util.*
import me.fzzyhmstrs.amethyst_core.nbt_util.Nbt
import me.fzzyhmstrs.amethyst_core.nbt_util.NbtKeys
import me.fzzyhmstrs.amethyst_core.registry.EventRegistry
import me.fzzyhmstrs.amethyst_core.scepter_util.base_augments.ScepterAugment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.TranslatableText
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import kotlin.NoSuchElementException
import kotlin.math.max

@Deprecated("refactoring and moving certain functions")
object ScepterHelper {

    private val augmentStats: MutableMap<String, AugmentDatapoint> = mutableMapOf()
    private val scepterHealTickers: MutableMap<ItemStack, EventRegistry.Ticker> = mutableMapOf()
    private val SCEPTER_SYNC_PACKET = Identifier(AC.MOD_ID,"scepter_sync_packet")
    val fallbackId = Identifier("vanishing_curse")

    fun initializeManaItem(stack: ItemStack){
        if (!scepterHealTickers.containsKey(stack)){
            val item = stack.item
            if (item is ManaItem) {
                scepterHealTickers[stack] = EventRegistry.Ticker(item.getRepairTime())
            }
        }
    }

    fun useScepter(activeEnchantId: String, activeEnchant: ScepterAugment, stack: ItemStack, world: World, cdMod: Double = 0.0): Int?{
        if (world !is ServerWorld){return null}
        val scepterNbt = stack.orCreateNbt
        if (EnchantmentHelper.getLevel(activeEnchant,stack) == 0){
            fixActiveEnchantWhenMissing(stack)
            return null
        }
        //cooldown modifier is a percentage modifier, so 20% will boost cooldown by 20%. -20% will take away 20% cooldown
        val cooldown = (augmentStats[activeEnchantId]?.cooldown?.times(100.0+ cdMod)?.div(100.0))?.toInt()
        val time = world.time

        val lastUsedList = NbtScepterHelper.getOrCreateLastUsedList(scepterNbt)
        val lastUsed = NbtScepterHelper.checkLastUsed(lastUsedList,activeEnchantId,time-1000000L)
        if (cooldown != null){
            val cooldown2 = max(cooldown,1) // don't let cooldown be less than 1 tick
            return if (time - cooldown2 >= lastUsed){ //checks that enough time has passed since last usage
                NbtScepterHelper.updateLastUsed(lastUsedList, activeEnchantId, time)
                cooldown2
            } else {
                null
            }
        }

        return null
    }

    fun sendScepterUpdateFromClient(up: Boolean) {
        val buf = PacketByteBufs.create()
        buf.writeBoolean(up)
        ClientPlayNetworking.send(SCEPTER_SYNC_PACKET,buf)
    }

    fun registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SCEPTER_SYNC_PACKET)
        { _: MinecraftServer,
          serverPlayerEntity: ServerPlayerEntity,
          _: ServerPlayNetworkHandler,
          packetByteBuf: PacketByteBuf,
          _: PacketSender ->
            val stack = serverPlayerEntity.getStackInHand(Hand.MAIN_HAND)
            val up = packetByteBuf.readBoolean()
            updateScepterActiveEnchant(stack,serverPlayerEntity,up)
        }
    }

    private fun updateScepterActiveEnchant(stack: ItemStack, user: PlayerEntity, up: Boolean){
        val item = stack.item
        if (item !is AbstractScepterItem) return
        val nbt = stack.orCreateNbt
        if (!nbt.contains(NbtKeys.ACTIVE_ENCHANT.str())){
            item.initializeScepter(stack, nbt)
        }
        val activeEnchantCheck = Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt)

        val activeCheck = Registry.ENCHANTMENT.get(Identifier(activeEnchantCheck))
        val activeEnchant = if (activeCheck != null) {
            if (EnchantmentHelper.getLevel(activeCheck, stack) == 0) {
                fixActiveEnchantWhenMissing(stack)
                Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt)
            } else {
                activeEnchantCheck
            }
        } else {
            fixActiveEnchantWhenMissing(stack)
            Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt)
        }

        val nbtEls = stack.enchantments
        var matchIndex = 0
        val augIndexes: MutableList<Int> = mutableListOf()
        for (i in 0..nbtEls.lastIndex){
            val identifier = EnchantmentHelper.getIdFromNbt(nbtEls[i] as NbtCompound)
            val enchantCheck = Registry.ENCHANTMENT.get(identifier)?: Enchantments.VANISHING_CURSE
            if(enchantCheck is ScepterAugment) {
                augIndexes.add(i)
            }
            if (activeEnchant == identifier?.toString()){
                matchIndex = i
            }
        }
        val newIndex = if (augIndexes.size != 0) {
            val augElIndex = if (augIndexes.indexOf(matchIndex) == -1){
                0
            } else {
                augIndexes.indexOf(matchIndex)
            }
            if (up) {
                augIndexes.getOrElse(augElIndex + 1) { 0 }
            } else {
                augIndexes.getOrElse(augElIndex - 1) { augIndexes[augIndexes.lastIndex] }
            }
        } else {
            0
        }
        val nbtTemp = nbtEls[newIndex] as NbtCompound
        val newActiveEnchant = EnchantmentHelper.getIdFromNbt(nbtTemp)?.toString()?:return
        val lastUsedList = NbtScepterHelper.getOrCreateLastUsedList(nbt)
        val currentTime = user.world.time
        val lastUsed: Long = NbtScepterHelper.checkLastUsed(lastUsedList,newActiveEnchant,currentTime-1000000L)
        val timeSinceLast = currentTime - lastUsed
        val cooldown = (augmentStats[newActiveEnchant]?.cooldown?:20).toLong()
        if(timeSinceLast >= cooldown){
            user.itemCooldownManager.remove(stack.item)
        } else{
            user.itemCooldownManager.set(stack.item, (cooldown - timeSinceLast).toInt())
        }
        ModifierHelper.DUSTBIN.markDirty(stack)
        Nbt.writeStringNbt(NbtKeys.ACTIVE_ENCHANT.str(),newActiveEnchant, nbt)
        val message = TranslatableText("scepter.new_active_spell").append(TranslatableText("enchantment.amethyst_imbuement.${Identifier(newActiveEnchant).path}"))
        user.sendMessage(message,false)
    }

    private fun fixActiveEnchantWhenMissing(stack: ItemStack) {
        val nbt = stack.orCreateNbt
        val item = stack.item
        if (item is AugmentScepterItem) {
            val newEnchant = EnchantmentHelper.get(stack).keys.firstOrNull()
            val identifier = if (newEnchant != null) {
                Registry.ENCHANTMENT.getId(newEnchant)
            } else {
                item.addDefaultEnchantment(stack)
                item.fallbackId
            }
            if (identifier != null) {
                nbt.putString(NbtKeys.ACTIVE_ENCHANT.str(), identifier.toString())
            }
            item.initializeScepter(stack, nbt)
        }
    }

    fun activeEnchantHelper(stack: ItemStack): String{
        val nbt: NbtCompound = stack.orCreateNbt
        return if (nbt.contains(NbtKeys.ACTIVE_ENCHANT.str())){
            Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt)
        } else {
            val item = stack.item
            if (item is AbstractScepterItem) {
                item.initializeScepter(stack,nbt)
            }
            Nbt.readStringNbt(NbtKeys.ACTIVE_ENCHANT.str(), nbt)
        }
    }

/*    fun checkManaCost(cost: Int, stack: ItemStack): Boolean{
        return (checkCanUseHandler(stack, cost))
    }

    fun applyManaCost(cost: Int, stack: ItemStack, world: World, user: PlayerEntity){
        damageHandler(stack,world,user,cost,LiteralText.EMPTY)
    }*/

    fun incrementScepterStats(scepterNbt: NbtCompound, activeEnchantId: String, xpMods: XpModifiers? = null){
        val spellKey = augmentStats[activeEnchantId]?.type?.name ?: return
        if(spellKey == SpellType.NULL.name) return
        val statLvl = Nbt.readIntNbt(spellKey + "_lvl",scepterNbt)
        val statMod = xpMods?.getMod(spellKey) ?: 0
        val statXp = Nbt.readIntNbt(spellKey + "_xp",scepterNbt) + statMod + 1
        Nbt.writeIntNbt(spellKey + "_xp",statXp,scepterNbt)
        if(checkXpForLevelUp(statXp,statLvl)){
            Nbt.writeIntNbt(spellKey + "_lvl",statLvl + 1,scepterNbt)
        }
    }

    fun getScepterStat(scepterNbt: NbtCompound, activeEnchantId: String): Pair<Int,Int>{
        val spellKey = augmentStats[activeEnchantId]?.type?.name ?: return Pair(1,0)
        val statLvl = Nbt.readIntNbt(spellKey + "_lvl",scepterNbt)
        val statXp = Nbt.readIntNbt(spellKey + "_xp",scepterNbt)
        return Pair(statLvl,statXp)
    }

    fun getScepterStats(stack: ItemStack): IntArray {
        val nbt = stack.orCreateNbt
        return getStatsHelper(nbt)
    }

    fun isAcceptableScepterItem(augment: ScepterAugment, stack: ItemStack, player: PlayerEntity): Boolean {
        val nbt = stack.orCreateNbt
        return checkScepterStat(
            nbt,
            Registry.ENCHANTMENT.getId(augment)?.toString() ?: ""
        ) || player.abilities.creativeMode
    }
    private fun checkScepterStat(scepterNbt: NbtCompound, activeEnchantId: String): Boolean{
        if (!augmentStats.containsKey(activeEnchantId)) return false
        val minLvl = augmentStats[activeEnchantId]?.minLvl?:return false
        val curLvl = getScepterStat(scepterNbt,activeEnchantId).first
        return (curLvl >= minLvl)
    }

    fun resetCooldown(world: World,stack: ItemStack, activeEnchantId: String){
        val nbt = stack.nbt?: return
        val lastUsedList = NbtScepterHelper.getOrCreateLastUsedList(nbt)
        val cd = augmentStats[activeEnchantId]?.cooldown?:20
        val currentLastUsed = NbtScepterHelper.checkLastUsed(lastUsedList,activeEnchantId, world.time)
        NbtScepterHelper.updateLastUsed(lastUsedList,activeEnchantId,currentLastUsed - cd - 2)
    }

    fun transferNbt(stack1: ItemStack,stack2: ItemStack){
        val nbt1 = stack1.nbt ?: return
        val nbt2 = stack2.orCreateNbt
        for(nbtKey in nbt1.keys){
            if(nbtKey == ItemStack.ENCHANTMENTS_KEY){
                continue
            }
            nbt2.put(nbtKey,nbt1[nbtKey])
        }
    }

    fun bookOfLoreNbtGenerator(tier: LoreTier = LoreTier.ANY_TIER): NbtCompound{
        val nbt = NbtCompound()
        val aug = getRandBookOfLoreAugment(tier.list())
        nbt.putString(NbtKeys.LORE_KEY.str(),aug)
        return nbt
    }
    private fun getRandBookOfLoreAugment(list: List<String>): String{
        if (list.isEmpty()) return fallbackId.toString()
        val rndMax = list.size
        val rndIndex = AC.acRandom.nextInt(rndMax)
        return list[rndIndex]
    }

    fun registerAugmentStat(id: String, dataPoint: AugmentDatapoint, overwrite: Boolean = false){
        if(!augmentStats.containsKey(id) || overwrite){
            augmentStats[id] = dataPoint
            dataPoint.bookOfLoreTier.addToList(id)
        }
    }
    fun registerAugmentStat(augment: ScepterAugment){
        val id = EnchantmentHelper.getEnchantmentId(augment)?.toString()?:throw NoSuchElementException("Enchantment ID for ${this.javaClass.canonicalName} not found!")
        val imbueLevel = if (checkAugmentStat(id)){
            getAugmentImbueLevel(id)
        } else {
            1
        }
        registerAugmentStat(id,configAugmentStat(augment,id,imbueLevel),true)
    }

    private fun configAugmentStat(augment: ScepterAugment, id: String, imbueLevel: Int = 1): AugmentDatapoint {
        val stat = augment.augmentStat(imbueLevel)
        val augmentConfig = ScepterAugment.Companion.AugmentStats()
        val type = stat.type
        augmentConfig.id = id
        augmentConfig.cooldown = stat.cooldown
        augmentConfig.manaCost = stat.manaCost
        augmentConfig.minLvl = stat.minLvl
        val tier = stat.bookOfLoreTier
        val item = stat.keyItem
        val augmentAfterConfig = ScepterAugment.configAugment(this.javaClass.simpleName + ScepterAugment.augmentVersion +".json",augmentConfig)
        return AugmentDatapoint(type,augmentAfterConfig.cooldown,augmentAfterConfig.manaCost,augmentAfterConfig.minLvl,imbueLevel,tier,item)
    }

    fun checkAugmentStat(id: String): Boolean{
        return augmentStats.containsKey(id)
    }

    fun getAugmentType(id: String): SpellType {
        if(!augmentStats.containsKey(id)) return SpellType.NULL
        return augmentStats[id]?.type?: SpellType.NULL
    }

    fun getAugmentItem(id: String): Item {
        if(!augmentStats.containsKey(id)) return Items.GOLD_INGOT
        return augmentStats[id]?.keyItem?:Items.GOLD_INGOT
    }

    fun getAugmentMinLvl(id: String): Int {
        if(!augmentStats.containsKey(id)) return 1
        return augmentStats[id]?.minLvl?:1
    }

    fun getAugmentManaCost(id: String, reduction: Double = 0.0): Int{
        if(!augmentStats.containsKey(id)) return (10 * (100.0 + reduction) / 100.0).toInt()
        val cost = (augmentStats[id]?.manaCost?.times(100.0 + reduction)?.div(100.0))?.toInt() ?: (10 * (100.0 + reduction) / 100.0).toInt()
        return max(1,cost)
    }

    fun getAugmentCooldown(id: String): Int{
        if(!augmentStats.containsKey(id)) return (20)
        val cd = (augmentStats[id]?.cooldown) ?: 20
        return max(1,cd)
    }

    fun getAugmentImbueLevel(id: String): Int{
        if(!augmentStats.containsKey(id)) return (1)
        val cd = (augmentStats[id]?.imbueLevel) ?: 1
        return max(1,cd)
    }

    fun getAugmentTier(id: String): LoreTier {
        if (!augmentStats.containsKey(id)) return (LoreTier.NO_TIER)
        return (augmentStats[id]?.bookOfLoreTier) ?: LoreTier.NO_TIER
    }

    fun xpToNextLevel(xp: Int,lvl: Int): Int{
        val xpNext = (2 * lvl * lvl + 40 * lvl)
        return (xpNext - xp + 1)
    }

    fun tickTicker(id: ItemStack): Boolean{
        val ticker = scepterHealTickers[id]?:return false
        ticker.tickUp()
        return ticker.isReady()
    }

    private fun checkXpForLevelUp(xp:Int,lvl:Int): Boolean{
        return (xp > (2 * lvl * lvl + 40 * lvl))
    }

    private fun getStatsHelper(nbt: NbtCompound): IntArray{
        val stats = intArrayOf(0,0,0,0,0,0)
        if(!nbt.contains("FURY_lvl")){
            nbt.putInt("FURY_lvl",1)
        }
        stats[0] = nbt.getInt("FURY_lvl")
        if(!nbt.contains("GRACE_lvl")){
            nbt.putInt("GRACE_lvl",1)
        }
        stats[1] = nbt.getInt("GRACE_lvl")
        if(!nbt.contains("WIT_lvl")){
            nbt.putInt("WIT_lvl",1)
        }
        stats[2] = nbt.getInt("WIT_lvl")
        if(!nbt.contains("FURY_xp")){
            nbt.putInt("FURY_xp",0)
        }
        stats[3] = nbt.getInt("FURY_xp")
        if(!nbt.contains("GRACE_xp")){
            nbt.putInt("GRACE_xp",0)
        }
        stats[4] = nbt.getInt("GRACE_xp")
        if(!nbt.contains("WIT_xp")){
            nbt.putInt("WIT_xp",0)
        }
        stats[5] = nbt.getInt("WIT_xp")
        return stats
    }

    object NbtScepterHelper {

        fun checkLastUsed(lastUsedList: NbtCompound, activeEnchantId: String, time: Long): Long{
            val key = activeEnchantId + NbtKeys.LAST_USED.str()
            return if (!lastUsedList.contains(key)) {
                Nbt.writeLongNbt(key, time, lastUsedList)
                time
            } else {
                Nbt.readLongNbt(key, lastUsedList)
            }
        }
        fun updateLastUsed(lastUsedList: NbtCompound, activeEnchantId: String, currentTime: Long){
            val key = activeEnchantId + NbtKeys.LAST_USED.str()
            Nbt.writeLongNbt(key, currentTime, lastUsedList)

        }

        fun getOrCreateLastUsedList(nbtCompound: NbtCompound): NbtCompound {
            val lastUsedList = nbtCompound.get(NbtKeys.LAST_USED_LIST.str())
            return if (lastUsedList == null){
                createLastUsedList(nbtCompound)
            } else {
                lastUsedList as NbtCompound
            }
        }

        private fun createLastUsedList(nbtCompound: NbtCompound): NbtCompound {
            val lastUsedList = NbtCompound()
            nbtCompound.put(NbtKeys.LAST_USED_LIST.str(),lastUsedList)
            return lastUsedList
        }


    }

}
