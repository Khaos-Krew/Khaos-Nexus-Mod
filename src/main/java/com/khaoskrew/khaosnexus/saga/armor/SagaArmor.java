package com.khaoskrew.khaosnexus.saga.armor;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

import com.khaoskrew.khaosnexus.KhaosNexus;
import com.khaoskrew.khaosnexus.registry.ModRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Hollow Crown armor sets (GeckoLib models):
 * Glass Choir (Vaelith): light, fast; full set = +8% speed and projectiles hit 20% softer.
 * Profane Vestments (Fallen Hierophant): sturdy; full set = +10% max health and melee attackers are set alight (30%).
 */
@EventBusSubscriber(modid = KhaosNexus.MODID)
public final class SagaArmor {
    private SagaArmor() {}

    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, KhaosNexus.MODID);

    static Holder<ArmorMaterial> material(String name, int[] def, float tough, float kb, java.util.function.Supplier<Ingredient> repair) {
        return MATERIALS.register(name, () -> {
            EnumMap<ArmorItem.Type, Integer> m = new EnumMap<>(ArmorItem.Type.class);
            m.put(ArmorItem.Type.BOOTS, def[0]); m.put(ArmorItem.Type.LEGGINGS, def[1]); m.put(ArmorItem.Type.CHESTPLATE, def[2]); m.put(ArmorItem.Type.HELMET, def[3]); m.put(ArmorItem.Type.BODY, def[2]);
            return new ArmorMaterial(m, 18, SoundEvents.ARMOR_EQUIP_DIAMOND, repair,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, name))), tough, kb);
        });
    }

    public static final Holder<ArmorMaterial> CHOIR = material("choir", new int[] {3, 6, 8, 3}, 2.5F, 0.0F, () -> Ingredient.of(ModRegistries.SINGING_GLASS.get()));
    public static final Holder<ArmorMaterial> VESTMENT = material("vestment", new int[] {3, 7, 8, 3}, 3.0F, 0.1F, () -> Ingredient.of(ModRegistries.PROFANE_SIGIL.get()));

    public static final DeferredItem<Item>[] CHOIR_SET = set("choir", CHOIR);
    public static final DeferredItem<Item>[] VESTMENT_SET = set("vestment", VESTMENT);

    @SuppressWarnings("unchecked")
    static DeferredItem<Item>[] set(String id, Holder<ArmorMaterial> mat) {
        ArmorItem.Type[] types = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};
        String[] names = {"helmet", "chestplate", "leggings", "boots"};
        DeferredItem<Item>[] out = new DeferredItem[4];
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type t = types[i];
            out[i] = ModRegistries.ITEMS.register(id + "_" + names[i], () -> new Piece(mat, t, id,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).durability(t.getDurability(37))));
        }
        return out;
    }

    public static void register(IEventBus bus) {
        MATERIALS.register(bus);
    }

    public static int worn(LivingEntity e, String set) {
        int n = 0;
        for (EquipmentSlot s : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (e.getItemBySlot(s).getItem() instanceof Piece p && p.set.equals(set)) n++;
        }
        return n;
    }

    static final ResourceLocation BONUS = ResourceLocation.fromNamespaceAndPath(KhaosNexus.MODID, "saga_set_bonus");

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || p.tickCount % 20 != 0) return;
        boolean choir = worn(p, "choir") == 4, vest = worn(p, "vestment") == 4;
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        var hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (speed != null) { speed.removeModifier(BONUS); if (choir) speed.addTransientModifier(new AttributeModifier(BONUS, 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
        if (hp != null) { hp.removeModifier(BONUS); if (vest) hp.addTransientModifier(new AttributeModifier(BONUS, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)); }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    @SubscribeEvent
    public static void hurt(LivingIncomingDamageEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        if (e.getSource().is(DamageTypeTags.IS_PROJECTILE) && worn(t, "choir") == 4) e.setAmount(e.getAmount() * 0.8F);
        if (worn(t, "vestment") == 4 && e.getSource().getDirectEntity() instanceof LivingEntity a && a == e.getSource().getEntity()
                && t.getRandom().nextFloat() < 0.3F) a.igniteForSeconds(3);
    }

    /** One armor piece; GeckoLib renders the whole set model and shows the bones for this slot. */
    public static class Piece extends ArmorItem implements GeoItem {
        public final String set;
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        public Piece(Holder<ArmorMaterial> m, Type t, String set, Properties p) {
            super(m, t, p);
            this.set = set;
        }

        @Override
        public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
            consumer.accept(new GeoRenderProvider() {
                private net.minecraft.client.model.HumanoidModel<?> renderer;

                @Override
                public <T extends LivingEntity> net.minecraft.client.model.HumanoidModel<?> getGeoArmorRenderer(T e, ItemStack s, EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<T> original) {
                    if (this.renderer == null) this.renderer = com.khaoskrew.khaosnexus.client.SagaArmorRenderer.create(Piece.this.set);
                    return this.renderer;
                }
            });
        }

        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {}
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

        @Override
        public void appendHoverText(ItemStack s, TooltipContext c, List<Component> t, TooltipFlag f) {
            t.add(Component.translatable("khaosnexus.armor." + this.set + ".set").withStyle(ChatFormatting.LIGHT_PURPLE));
            t.add(Component.translatable("khaosnexus.armor." + this.set + ".bonus").withStyle(ChatFormatting.GRAY));
        }
    }
}
