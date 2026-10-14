package com.khaoskrew.khaosnexus.saga;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 1.2 audio: original procedural tracks and effects from tools/gen_sounds.py (CC0, see docs/AUDIO_LICENSE.md). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, KhaosNexus.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> KING_ROAR = reg("king.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> KING_PHASE = reg("king.phase");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROWNFALL_RISE = reg("king.crownfall_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROWNFALL_IMPACT = reg("king.crownfall_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHARD_STRIKE = reg("king.shard_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> KING_DEATH = reg("king.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_KING = reg("music.hollow_king");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_CAMP = reg("music.underthrone_camp");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, id)));
    }

    public static Holder<SoundEvent> holder(DeferredHolder<SoundEvent, SoundEvent> h) { return h; }
}
