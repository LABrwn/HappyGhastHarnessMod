package com.happyghastharness;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class HappyGhastHarnessMod implements ModInitializer {
	public static final String MOD_ID = "happyghastharness";
	public static final String SOUL_SPEED_TRANSLATION_KEY = "enchantment.minecraft.soul_speed";

	/** Soul Speed Stufe 1 / 2 / 3 -> 1,5x / 2x / 2,5x Fluggeschwindigkeit. */
	private static final double[] SPEED_BONUS_PER_LEVEL = {0.0, 0.5, 1.0, 1.5};
	private static final int UPDATE_INTERVAL_TICKS = 5;

	private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "soul_speed_harness");

	/** Ghasts, auf die gerade unser Modifier angewendet ist (schwach referenziert). */
	private final Set<LivingEntity> boosted = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (LivingEntity ghast : new HashSet<>(boosted)) {
				removeBoost(ghast);
			}
			boosted.clear();
		});
	}

	private void onServerTick(MinecraftServer server) {
		if (server.getTickCount() % UPDATE_INTERVAL_TICKS != 0) {
			return;
		}

		Set<LivingEntity> activeNow = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Entity vehicle = player.getVehicle();
			if (vehicle instanceof LivingEntity ghast && vehicle.getType() == EntityType.HAPPY_GHAST) {
				int level = getSoulSpeedLevel(ghast);
				if (level > 0) {
					applyBoost(ghast, SPEED_BONUS_PER_LEVEL[Math.min(level, 3)]);
					activeNow.add(ghast);
				}
			}
		}

		// Alle Ghasts, die nicht mehr geritten werden oder kein Soul-Speed-Harness mehr haben: Boost entfernen.
		for (LivingEntity ghast : new HashSet<>(boosted)) {
			if (!activeNow.contains(ghast)) {
				removeBoost(ghast);
				boosted.remove(ghast);
			}
		}
		boosted.addAll(activeNow);
	}

	private static int getSoulSpeedLevel(LivingEntity ghast) {
		ItemStack harness = ghast.getItemBySlot(EquipmentSlot.BODY);
		if (harness.isEmpty() || !harness.is(ItemTags.HARNESSES)) {
			return 0;
		}
		Holder<Enchantment> soulSpeed = ghast.level().registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SOUL_SPEED);
		return EnchantmentHelper.getItemEnchantmentLevel(soulSpeed, harness);
	}

	private static void applyBoost(LivingEntity ghast, double bonus) {
		AttributeInstance speed = ghast.getAttribute(Attributes.FLYING_SPEED);
		if (speed == null) {
			return;
		}
		AttributeModifier existing = speed.getModifier(MODIFIER_ID);
		if (existing != null) {
			if (existing.amount() == bonus) {
				return;
			}
			speed.removeModifier(MODIFIER_ID);
		}
		// ADD_MULTIPLIED_TOTAL: Wert * (1 + bonus). Transient = wird nicht in der Welt gespeichert.
		speed.addTransientModifier(new AttributeModifier(MODIFIER_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void removeBoost(LivingEntity ghast) {
		AttributeInstance speed = ghast.getAttribute(Attributes.FLYING_SPEED);
		if (speed != null) {
			speed.removeModifier(MODIFIER_ID);
		}
	}
}
