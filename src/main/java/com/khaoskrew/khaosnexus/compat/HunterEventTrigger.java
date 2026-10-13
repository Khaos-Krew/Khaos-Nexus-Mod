package com.khaoskrew.khaosnexus.compat;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Advancement trigger {@code khaosnexus:hunter_event}. Works without any other mod; quest mods
 * (FTB Quests advancement tasks, etc.) can point at datapack advancements that use it.
 *
 * <pre>{"trigger":"khaosnexus:hunter_event","conditions":{"event":"gate_cleared","value":"s"}}</pre>
 * Optional "value" matches a string (rank, class id, title id, bounty id, boss id) and
 * optional "min" requires the event's number (rank tier 0-5, Spire floor) to be at least that.
 */
public class HunterEventTrigger extends SimpleCriterionTrigger<HunterEventTrigger.Instance> {
    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event, String value, int number) {
        this.trigger(player, i -> i.matches(event, value, number));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String event, Optional<String> value, Optional<Integer> min)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(b -> b.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event),
                Codec.STRING.optionalFieldOf("value").forGetter(Instance::value),
                Codec.INT.optionalFieldOf("min").forGetter(Instance::min)).apply(b, Instance::new));

        boolean matches(String event, String value, int number) {
            return this.event.equals(event)
                    && this.value.map(v -> v.equalsIgnoreCase(value)).orElse(true)
                    && this.min.map(m -> number >= m).orElse(true);
        }
    }
}
