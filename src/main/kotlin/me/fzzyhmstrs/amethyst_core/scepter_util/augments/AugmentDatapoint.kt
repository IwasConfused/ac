package me.fzzyhmstrs.amethyst_core.scepter_util.augments

import me.fzzyhmstrs.amethyst_core.scepter_util.LoreTier
import me.fzzyhmstrs.amethyst_core.scepter_util.SpellType
import net.minecraft.item.Item
import net.minecraft.item.Items

/**
 * Data class defines the characteristics of a [ScepterAugment].
 *
 * [type]: The [SpellType] for the augment. This both organizes the spell flavor-wise, and also tells an Augment Scepter to increment the correct level stat. See [SpellType] for recommendations of using the three types.
 *
 * [cooldown]: time in ticks between each spell cast.
 *
 * [manaCost]: the mana damage inflicted on the Augment Scepter with each successful cast
 *
 * [minLvl]: the minimum [SpellType] level the scepter must be before the augment can be added to the scepter
 *
 * [imbueLevel]: The cost in Experience Levels to imbue this augment on the Imbuing Table. Typically defined in the Imbuing Recipe json
 *
 * [bookOfLoreTier]: Defines which Knowledge Book the augment can show up in. see [LoreTier] for more info. "NO_TIER" will not add the augment to any book. Typically used for augments that have pre-available crafting recipes the player can use without a Knowledge Book.
 *
 * [keyItem]: If the recipe follows a templated pattern, setting this will give the player a hint as to how to craft this augment without REI or similar.
 *
 * [enabled]: (new in 0.3.1) Determines if the augment is enabled by the configuration settings. If not enabled, a registry can opt to skip it or otherwise exclude it from showing up during play.
 */

data class AugmentDatapoint(val type: SpellType = SpellType.NULL, val cooldown: Int = 20,
                            val manaCost: Int = 20, val minLvl: Int = 1, val imbueLevel: Int = 1,
                            val bookOfLoreTier: LoreTier = LoreTier.NO_TIER,
                            val keyItem: Item = Items.AIR, val enabled: Boolean = true
)