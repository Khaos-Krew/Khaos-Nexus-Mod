package com.khaoskrew.khaosnexus.card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.NexusGateBlock;
import com.khaoskrew.khaosnexus.gate.NexusGateBlockEntity;
import com.khaoskrew.khaosnexus.network.CardPayload;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

public class HunterCardItem extends Item {
    public HunterCardItem(Properties properties) {
        super(properties);
    }

    @org.jetbrains.annotations.Nullable
    public static UUID owner(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }

    public static void bind(ItemStack stack, Player player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID("Owner", player.getUUID());
            tag.putString("OwnerName", player.getGameProfile().getName());
        });
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        // Picking the card up (from a trade or anywhere) binds it and unlocks the hunter UI.
        if (!level.isClientSide && entity instanceof ServerPlayer player && level.getGameTime() % 20L == 0L) {
            if (owner(stack) == null) {
                bind(stack, player);
            }
            if (player.getUUID().equals(owner(stack))) {
                HunterCard.unlock(player);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        return open(server, stack) ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    /** Loaded Nexus Gates within the radius, nearest first. */
    public static List<CardPayload.Gate> nearbyGates(ServerLevel level, BlockPos center, int radius) {
        List<CardPayload.Gate> out = new ArrayList<>();
        int r = (radius >> 4) + 1;
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof NexusGateBlockEntity gate) {
                        BlockPos p = gate.getBlockPos();
                        double d = Math.sqrt(p.distSqr(center));
                        if (d <= radius) {
                            boolean closed = gate.isClosed() || gate.getBlockState().getValue(NexusGateBlock.CLOSED);
                            out.add(new CardPayload.Gate(gate.getRank().name(), p, (int) Math.round(d),
                                    closed ? 2 : gate.isActive() ? 1 : 0, com.khaoskrew.khaosnexus.dungeon.DungeonThemes.nameKey(gate.getTheme())));
                        }
                    }
                }
            }
        }
        out.sort((a, b) -> Integer.compare(a.distance(), b.distance()));
        return out.size() > 64 ? out.subList(0, 64) : out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("OwnerName");
        tooltip.add(name.isEmpty()
                ? Component.translatable("khaosnexus.card.unbound").withStyle(ChatFormatting.GRAY)
                : Component.translatable("khaosnexus.card.bound", name).withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("khaosnexus.card.hint").withStyle(ChatFormatting.GRAY));
    }

    /** Bind if unowned, check ownership, unlock and send the card screen. Used by right-click and the open-card key. */
    public static boolean open(ServerPlayer server, ItemStack stack) {
        UUID owner = owner(stack);
        if (owner == null) {
            bind(stack, server);
        } else if (!owner.equals(server.getUUID())) {
            String name = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getString("OwnerName");
            server.displayClientMessage(Component.translatable("khaosnexus.card.not_yours", name), true);
            return false;
        }
        HunterCard.unlock(server);
        int radius = Config.CARD_GATE_RADIUS.get();
        PacketDistributor.sendToPlayer(server, new CardPayload(nearbyGates((ServerLevel) server.level(), server.blockPosition(), radius), radius,
                com.khaoskrew.khaosnexus.forge.ForgeRecipes.cardLines(server)));
        return true;
    }
}
