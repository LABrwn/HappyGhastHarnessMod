package com.happyghastharness.mixin;

import com.happyghastharness.HappyGhastHarnessMod;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Macht Soul Speed fuer Happy-Ghast-Harnesses "unterstuetzt".
 *
 * Der Amboss prueft {@code isSupportedItem}. Der Verzauberungstisch nutzt die Primary-Items und die
 * Tag {@code in_enchanting_table}; Soul Speed ist dort nicht enthalten. Dadurch geht es nur per Amboss.
 */
@Mixin(Enchantment.class)
public abstract class EnchantmentMixin {

	@Inject(method = "isSupportedItem", at = @At("HEAD"), cancellable = true)
	private void happyghastharness$allowSoulSpeedOnHarness(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (!stack.is(ItemTags.HARNESSES)) {
			return;
		}
		Enchantment self = (Enchantment) (Object) this;
		if (self.description().getContents() instanceof TranslatableContents contents
				&& HappyGhastHarnessMod.SOUL_SPEED_TRANSLATION_KEY.equals(contents.getKey())) {
			cir.setReturnValue(true);
		}
	}
}
