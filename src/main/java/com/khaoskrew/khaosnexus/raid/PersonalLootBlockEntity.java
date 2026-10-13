package com.khaoskrew.khaosnexus.raid;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PersonalLootBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String itemId = "minecraft:diamond";
    private int count = 1;
    private boolean given;

    public PersonalLootBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.PERSONAL_LOOT_ENTITY.get(), pos, state);
    }

    public void setReward(UUID owner, String itemId, int count) {
        this.owner = owner;
        this.itemId = itemId;
        this.count = count;
        this.setChanged();
    }

    public boolean ownedBy(Player player) {
        return this.owner != null && this.owner.equals(player.getUUID());
    }

    public void give(Player player) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(this.itemId)).orElse(Items.DIAMOND);
        ItemStack stack = new ItemStack(item, Math.max(1, this.count));
        this.given = true;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.owner != null) {
            tag.putString("Owner", this.owner.toString());
        }
        tag.putString("Item", this.itemId);
        tag.putInt("Count", this.count);
        tag.putBoolean("Given", this.given);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Owner")) {
            try {
                this.owner = UUID.fromString(tag.getString("Owner"));
            } catch (IllegalArgumentException ignored) {
                this.owner = null;
            }
        }
        this.itemId = tag.getString("Item");
        this.count = tag.getInt("Count");
        this.given = tag.getBoolean("Given");
    }
}
