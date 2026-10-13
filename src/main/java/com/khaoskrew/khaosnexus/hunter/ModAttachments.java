package com.khaoskrew.khaosnexus.hunter;

import com.khaoskrew.khaosnexus.KhaosNexus;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, KhaosNexus.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<AttachmentType<?>, AttachmentType<HunterProfile>> HUNTER =
            ATTACHMENT_TYPES.register("hunter", () -> AttachmentType.serializable(HunterProfile::new)
                    .copyOnDeath()
                    .sync((holder, player) -> NetworkRegistry.hasChannel(
                            player.connection,
                            ResourceLocation.fromNamespaceAndPath("neoforge", "sync_attachments")),
                            HunterProfile.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
