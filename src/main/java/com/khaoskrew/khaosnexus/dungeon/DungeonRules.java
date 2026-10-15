package com.khaoskrew.khaosnexus.dungeon;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.spire.SpireService;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Flight and build rules inside gate dungeons and the Spire. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class DungeonRules {
    public static final ResourceKey<Level> GATE_DUNGEON = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "gate_dungeon"));

    private DungeonRules() {}

    public static boolean isDungeon(Level level) {
        ResourceKey<Level> key = level.dimension();
        return key == GATE_DUNGEON || key == SpireService.DIMENSION;
    }

    static boolean exempt(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    private static boolean ruled(Player player, boolean toggle) {
        return toggle && !exempt(player) && isDungeon(player.level());
    }

    private static final ResourceLocation NO_FLIGHT = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "dungeon_no_flight");

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        boolean rule = ruled(player, Config.DUNGEON_NO_FLIGHT.get());
        // Mods that grant flight through NeoForge's creative_flight attribute are zeroed while the rule applies.
        var flight = player.getAttribute(net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            if (rule && !flight.hasModifier(NO_FLIGHT)) {
                flight.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(NO_FLIGHT, -1.0D,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!rule && flight.hasModifier(NO_FLIGHT)) {
                flight.removeModifier(NO_FLIGHT);
            }
        }
        if (!rule) {
            return;
        }
        boolean changed = false;
        if (player.getAbilities().mayfly || player.getAbilities().flying) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            changed = true;
        }
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }
        if (changed) {
            player.onUpdateAbilities();
        }
    }

    /** Explosions (creepers, mob abilities, TNT carts) never break blocks inside gate dungeons or the Spire. */
    @SubscribeEvent
    public static void onExplode(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (Config.DUNGEON_NO_BUILD.get() && isDungeon(event.getLevel())) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (ruled(event.getPlayer(), Config.DUNGEON_NO_BUILD.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && ruled(player, Config.DUNGEON_NO_BUILD.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        // Stops the client-side mining animation and instant-break blocks too.
        if (ruled(event.getEntity(), Config.DUNGEON_NO_BUILD.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!ruled(player, Config.DUNGEON_NO_BUILD.get())) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (interactable(state)) {
            // Let the block be used, but never let the held item place or modify anything.
            event.setUseItem(TriState.FALSE);
        } else {
            // Buckets, flint and steel, hoes, axes stripping and block items.
            event.setUseItem(TriState.FALSE);
            event.setUseBlock(TriState.FALSE);
        }
    }

    public static boolean interactable(BlockState state) {
        var b = state.getBlock();
        return b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock
                || b instanceof LeverBlock || b instanceof ButtonBlock || b instanceof ChestBlock
                || b instanceof BarrelBlock || b instanceof EnderChestBlock || b instanceof ShulkerBoxBlock
                || state.hasProperty(BlockStateProperties.OPEN);
    }
}
