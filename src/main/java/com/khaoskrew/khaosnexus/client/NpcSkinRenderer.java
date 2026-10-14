package com.khaoskrew.khaosnexus.client;

import com.khaoskrew.khaosnexus.KhaosNexus;
import java.util.Set;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

/** Wandering Trader renderer that gives the mod's named NPCs (Spire vendors, exile camp) their own skins. The synced
 * custom name's translation key ({@code khaosnexus.vendor.smith} → {@code textures/entity/npc/vendor_smith.png}) picks the skin. */
public class NpcSkinRenderer extends WanderingTraderRenderer {
    static final Set<String> SKINS = Set.of("vendor_alchemist", "vendor_armorer", "vendor_smith", "exile_captain", "exile_keeper", "exile_quartermaster");

    public NpcSkinRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** Skin id for a name, or null for an ordinary trader. */
    public static String skinFor(Component name) {
        if (name != null && name.getContents() instanceof TranslatableContents tc && tc.getKey().startsWith(KhaosNexus.MODID + ".")) {
            String[] k = tc.getKey().split("\\.");
            String id = k.length >= 3 ? k[1] + "_" + k[2] : "";
            return SKINS.contains(id) ? id : null;
        }
        return null;
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingTrader trader) {
        String id = skinFor(trader.getCustomName());
        return id == null ? super.getTextureLocation(trader)
                : ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "textures/entity/npc/" + id + ".png");
    }
}
