package com.khaoskrew.khaosnexus.endgame;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Generic datapack folder of JSON objects, reloaded with /reload. Used by the later endgame features. */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class JsonPool extends SimpleJsonResourceReloadListener {
    public static final JsonPool SPIRE_AFFIXES = new JsonPool("khaosnexus_spire_affixes");
    public static final JsonPool AURAS = new JsonPool("khaosnexus_auras");
    public static final JsonPool MASTERY = new JsonPool("khaosnexus_mastery");
    public static final JsonPool BOSS_RUSH = new JsonPool("khaosnexus_boss_rush");
    public static final JsonPool CHALLENGES = new JsonPool("khaosnexus_challenges");
    public static final JsonPool AWAKENINGS = new JsonPool("khaosnexus_awakenings");
    public static final JsonPool RAID_BOSSES = new JsonPool("khaosnexus_raid_bosses");
    public static final JsonPool SPIRE_THEMES = new JsonPool("khaosnexus_spire_themes");
    public static final JsonPool SPIRE_VENDORS = new JsonPool("khaosnexus_spire_vendors");

    private final String folder;
    private final Map<ResourceLocation, JsonObject> entries = new LinkedHashMap<>();

    private JsonPool(String folder) {
        super(new Gson(), folder);
        this.folder = folder;
    }

    public Map<ResourceLocation, JsonObject> all() {
        return this.entries;
    }

    public JsonObject get(ResourceLocation id) {
        return this.entries.get(id);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        this.entries.clear();
        files.forEach((id, json) -> {
            if (json.isJsonObject()) {
                this.entries.put(id, json.getAsJsonObject());
            } else {
                KhaosNexus.LOGGER.warn("Ignoring non-object {} in {}", id, this.folder);
            }
        });
        KhaosNexus.LOGGER.info("Loaded {} entries from {}", this.entries.size(), this.folder);
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(SPIRE_AFFIXES);
        event.addListener(AURAS);
        event.addListener(MASTERY);
        event.addListener(BOSS_RUSH);
        event.addListener(CHALLENGES);
        event.addListener(AWAKENINGS);
        event.addListener(RAID_BOSSES);
        event.addListener(SPIRE_THEMES);
        event.addListener(SPIRE_VENDORS);
    }
}
