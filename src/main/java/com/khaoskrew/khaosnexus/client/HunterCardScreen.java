package com.khaoskrew.khaosnexus.client;

import java.util.List;

import com.khaoskrew.khaosnexus.Config;
import com.khaoskrew.khaosnexus.gate.GateRank;
import com.khaoskrew.khaosnexus.hunter.HunterClass;
import com.khaoskrew.khaosnexus.hunter.HunterProfile;
import com.khaoskrew.khaosnexus.hunter.HunterRanks;
import com.khaoskrew.khaosnexus.hunter.ModAttachments;
import com.khaoskrew.khaosnexus.network.CardPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Hunter Card: profile, nearby gates and client display settings. */
public class HunterCardScreen extends Screen {
    private static final int W = 300;
    private static final int H = 200;
    private static int lastTab;
    private final List<CardPayload.Gate> gates;
    private final int radius;
    private final List<CardPayload.ForgeLine> forge;
    private final CardPayload.SkillHeader skillHeader;
    private final List<CardPayload.SkillLine> skills;
    private int tab;
    private int guildTab = -1;
    private boolean prestigeArmed;
    private int scroll;

    public HunterCardScreen(CardPayload payload) {
        super(Component.translatable("khaosnexus.card.title"));
        this.gates = payload.gates();
        this.radius = payload.radius();
        this.forge = payload.forge();
        this.skillHeader = payload.skillHeader();
        this.skills = payload.skills();
        this.tab = lastTab;
    }

    public static void open(CardPayload payload) {
        Minecraft.getInstance().setScreen(new HunterCardScreen(payload));
    }

    private float scale() {
        return Config.UI_SCALE.get().floatValue();
    }

    private int left() {
        return (int) ((this.width / scale() - W) / 2);
    }

    private int top() {
        return (int) ((this.height / scale() - H) / 2);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        float k = scale();
        int x = (int) (left() * k);
        int y = (int) ((top() - 22) * k);
        String[] tabs = {"khaosnexus.card.tab.info", "khaosnexus.card.tab.gates", "khaosnexus.card.tab.settings", "khaosnexus.card.tab.forge", "khaosnexus.card.tab.skills", "khaosnexus.card.tab.guild"};
        this.guildTab = tabs.length - 1;
        int tw = (int) (W * k / tabs.length); // tabs span the scaled card width
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                this.tab = t;
                lastTab = t;
                this.scroll = 0;
                this.init();
            }).bounds(x + i * tw, y, tw - 2, 18).build();
            b.active = i != this.tab;
            this.addRenderableWidget(b);
        }
        if (this.tab == this.guildTab) {
            this.guildWidgets(k);
        }
        if (this.tab == 2) {
            int sx = this.width / 2 - 100;
            int sy = (int) ((top() + 34) * k);
            this.addRenderableWidget(new Slider(sx, sy, "khaosnexus.card.set.hud_scale", Config.HUD_SCALE, 0.5D, 2.0D, false));
            this.addRenderableWidget(new Slider(sx, sy + 26, "khaosnexus.card.set.hud_opacity", Config.HUD_OPACITY, 0.0D, 1.0D, true));
            this.addRenderableWidget(new Slider(sx, sy + 52, "khaosnexus.card.set.ui_scale", Config.UI_SCALE, 0.6D, 1.6D, false));
            this.addRenderableWidget(new Slider(sx, sy + 78, "khaosnexus.card.set.ui_opacity", Config.UI_OPACITY, 0.2D, 1.0D, true));
            this.addRenderableWidget(Button.builder(Component.translatable("khaosnexus.card.set.apply"), btn -> this.init())
                    .bounds(sx, sy + 110, 200, 18).build());
        }
        if (this.tab == 4) {
            this.skillWidgets(k);
        }
    }

    private static final int[] STATE_COLOR = {0xFFF2EEF8, 0xFF8A6A6A, 0xFF8A7A9A, 0xFFE0C070, 0xFFB0A0C0};

    private Component skillName(CardPayload.SkillLine l) {
        return Component.translatable("skill.khaosnexus." + this.skillHeader.classId() + "." + l.id());
    }

    private void skillWidgets(float k) {
        String cls = this.skillHeader.classId();
        for (CardPayload.SkillLine l : this.skills) {
            int bx = (int) ((left() + 8 + l.x() * 96) * k);
            int by = (int) ((top() + 40 + l.y() * 30) * k);
            String label = this.font.plainSubstrByWidth(skillName(l).getString(), 70) + " " + l.rank() + "/" + l.maxRank();
            Button b = Button.builder(Component.literal(label).withColor(STATE_COLOR[l.state()]), btn ->
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.khaoskrew.khaosnexus.network.ActionPayload(com.khaoskrew.khaosnexus.network.ActionPayload.SKILL_SPEND, l.id())))
                    .bounds(bx, by, (int) (92 * k), (int) (20 * k)).build();
            net.minecraft.network.chat.MutableComponent tip = skillName(l).copy().withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                    .append("\n").append(Component.translatable("skill.khaosnexus." + cls + "." + l.id() + ".desc").withStyle(net.minecraft.ChatFormatting.GRAY))
                    .append("\n").append(Component.translatable("khaosnexus.card.skill_rank", l.rank(), l.maxRank()))
                    .append("\n").append(Component.translatable("khaosnexus.card.skill_cost", l.cost()));
            if (l.state() == 1) {
                tip.append("\n").append(Component.translatable("khaosnexus.card.skill_locked_rank", l.minRank()).withStyle(net.minecraft.ChatFormatting.RED));
            } else if (l.state() == 2) {
                tip.append("\n").append(Component.translatable("khaosnexus.card.skill_needs").withStyle(net.minecraft.ChatFormatting.RED));
            }
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tip));
            b.active = l.state() == 0;
            this.addRenderableWidget(b);
        }
        if (!this.skills.isEmpty()) {
            Component cost = this.skillHeader.respecItem().isEmpty() ? Component.translatable("khaosnexus.card.respec_free")
                    : Component.translatable("khaosnexus.card.respec_cost",
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocationHelper.parse(this.skillHeader.respecItem())).getDescription(),
                    this.skillHeader.respecCount());
            Button r = Button.builder(Component.translatable("khaosnexus.card.respec"), btn ->
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                            new com.khaoskrew.khaosnexus.network.ActionPayload(com.khaoskrew.khaosnexus.network.ActionPayload.SKILL_RESPEC, "")))
                    .bounds((int) ((left() + W - 78) * k), (int) ((top() + H - 26) * k), (int) (70 * k), (int) (18 * k)).build();
            r.setTooltip(net.minecraft.client.gui.components.Tooltip.create(cost));
            this.addRenderableWidget(r);
        }
    }

    private static final class ResourceLocationHelper {
        static net.minecraft.resources.ResourceLocation parse(String s) {
            return net.minecraft.resources.ResourceLocation.parse(s);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int size = this.tab == 3 ? this.forge.size() : this.gates.size();
        this.scroll = Math.max(0, Math.min(Math.max(0, size - 12), this.scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        float k = scale();
        int a = (int) (255 * Config.UI_OPACITY.get());
        g.pose().pushPose();
        g.pose().translate(0, 0, -10);
        g.pose().scale(k, k, 1);
        int x = left();
        int y = top();
        g.fill(x, y, x + W, y + H, (a << 24) | 0x0E0816);
        g.fill(x, y, x + W, y + 1, 0xFFB44BE6);
        g.fill(x, y + H - 1, x + W, y + H, 0xFFC42E44);
        g.fill(x, y, x + 1, y + H, 0xFF5A2A7A);
        g.fill(x + W - 1, y, x + W, y + H, 0xFF5A2A7A);
        HunterProfile p = this.minecraft.player.getData(ModAttachments.HUNTER);
        g.drawString(this.font, Component.translatable("khaosnexus.card.header", this.minecraft.player.getName()), x + 8, y + 8, 0xFFE8D8FF, true);
        if (this.tab == 0) {
            this.info(g, p, x + 8, y + 24);
        } else if (this.tab == 1) {
            this.gateList(g, x + 8, y + 24);
        } else if (this.tab == 3) {
            this.forgeList(g, x + 8, y + 24);
        } else if (this.tab == 4) {
            if (this.skills.isEmpty()) {
                g.drawString(this.font, Component.translatable("khaosnexus.card.no_class_skills"), x + 8, y + 24, 0xFFA898B8, false);
            } else {
                g.drawString(this.font, Component.translatable("khaosnexus.card.skills_header", this.skillHeader.level(),
                        this.skillHeader.free(), this.skillHeader.total()), x + 8, y + 24, 0xFFE0C070, false);
                for (CardPayload.SkillLine l : this.skills) { // connector ticks to prerequisites, drawn under the buttons
                    if (l.y() > 0 && l.x() < 3) {
                        int cx = x + 8 + l.x() * 96 + 46;
                        g.fill(cx, y + 40 + l.y() * 30 - 10, cx + 1, y + 40 + l.y() * 30, 0xFF5A2A7A);
                    }
                }
            }
        } else if (this.tab == this.guildTab) {
            g.drawString(this.font, Component.translatable("khaosnexus.card.guild.note"), x + 8, y + 20, 0xFFA898B8, false);
        } else {
            g.drawString(this.font, Component.translatable("khaosnexus.card.set.note"), x + 8, y + 20, 0xFFA898B8, false);
        }
        g.pose().popPose();
    }

    private void line(GuiGraphics g, int x, int y, Component label, Component value) {
        g.drawString(this.font, label, x, y, 0xFFA898B8, false);
        g.drawString(this.font, value, x + 96, y, 0xFFF2EEF8, false);
    }

    private void info(GuiGraphics g, HunterProfile p, int x, int y) {
        GateRank rank = HunterRanks.rankOfXp(p.xp);
        GateRank next = rank.ordinal() + 1 < GateRank.values().length ? GateRank.values()[rank.ordinal() + 1] : null;
        HunterClass c = HunterClass.byId(p.classId);
        int lo = HunterRanks.threshold(rank);
        int hi = next == null ? lo : HunterRanks.threshold(next);
        int prestige = p.endgame.getInt("Prestige");
        line(g, x, y, Component.translatable("khaosnexus.card.rank"), prestige <= 0 ? Component.translatable(rank.translationKey())
                : Component.translatable(prestige >= 2 ? "khaosnexus.prestige.national" : "khaosnexus.prestige.ss").copy()
                        .withStyle(prestige >= 2 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.LIGHT_PURPLE));
        line(g, x, y + 12, Component.translatable("khaosnexus.card.level"),
                Component.translatable("khaosnexus.card.level_value", com.khaoskrew.khaosnexus.skill.SkillService.level(p.xp), p.xp, next == null ? "-" : String.valueOf(hi)));
        int bar = next == null ? 180 : (int) (180 * Math.max(0, Math.min(1, (p.xp - lo) / (double) Math.max(1, hi - lo))));
        g.fill(x + 96, y + 23, x + 276, y + 26, 0xFF2A1A3A);
        g.fill(x + 96, y + 23, x + 96 + bar, y + 26, 0xFFB44BE6);
        line(g, x, y + 30, Component.translatable("khaosnexus.card.class"),
                c == null ? Component.translatable("khaosnexus.hud.no_class") : Component.translatable(c.translationKey()));
        line(g, x, y + 42, Component.translatable("khaosnexus.card.title_label"),
                p.title.isEmpty() ? Component.literal("-") : Component.translatable("khaosnexus.title." + p.title));
        g.drawString(this.font, Component.translatable("khaosnexus.card.skills"), x, y + 60, 0xFFC42E44, false);
        if (c != null) {
            line(g, x, y + 72, Component.translatable("khaosnexus.card.skill_primary"), Component.translatable(c.abilityKey()));
            line(g, x, y + 84, Component.translatable("khaosnexus.card.skill_alt"), Component.translatable(c.altAbilityKey()));
        } else {
            g.drawString(this.font, Component.translatable("khaosnexus.card.no_skills"), x, y + 72, 0xFF8A7A9A, false);
        }
        g.drawString(this.font, Component.translatable("khaosnexus.card.stats"), x, y + 102, 0xFFC42E44, false);
        line(g, x, y + 114, Component.translatable("khaosnexus.card.gates_cleared"), Component.literal(String.valueOf(p.gatesCleared)));
        line(g, x, y + 126, Component.translatable("khaosnexus.card.raids_cleared"), Component.literal(String.valueOf(p.raidsCleared)));
        line(g, x, y + 138, Component.translatable("khaosnexus.card.spire"),
                Component.translatable("khaosnexus.card.spire_value2", p.spireFloor, Math.max(1, p.endgame.getInt("SpireLevel")), p.spireBest));
        line(g, x, y + 150, Component.translatable("khaosnexus.card.party"), Component.literal(String.valueOf(p.partySize)));
        String aura = p.endgame.getString("Aura");
        if (!aura.isEmpty()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(aura);
            line(g, x, y + 162, Component.translatable("khaosnexus.card.aura"),
                    Component.translatable("khaosnexus.aura." + id.getNamespace() + "." + id.getPath()));
        }
    }

    private void forgeList(GuiGraphics g, int x, int y) {
        g.drawString(this.font, Component.translatable("khaosnexus.card.forge_header"), x, y, 0xFFA898B8, false);
        if (this.forge.isEmpty()) {
            g.drawString(this.font, Component.translatable("khaosnexus.card.no_forge"), x, y + 16, 0xFF8A7A9A, false);
            return;
        }
        int row = y + 16;
        for (int i = this.scroll; i < Math.min(this.forge.size(), this.scroll + 12); i++) {
            CardPayload.ForgeLine l = this.forge.get(i);
            int text = l.unlocked() ? 0xFFF2EEF8 : 0xFF6A5A7A;
            g.drawString(this.font, Component.translatable(l.name()), x, row + 1, text, false);
            g.drawString(this.font, Component.literal("+" + l.level()), x + 62, row + 1, 0xFFB44BE6, false);
            GateRank r = GateRank.byName(l.rank());
            g.fill(x + 80, row, x + 96, row + 10, 0xFF1A1024);
            g.drawCenteredString(this.font, l.rank(), x + 88, row + 1, l.unlocked() ? 0xFF000000 | r.color() : 0xFF5A4A6A);
            Component cost = Component.translatable("khaosnexus.card.forge_cost", l.count(), l.tier(), l.essence());
            if (l.core() > 0) {
                cost = cost.copy().append(Component.translatable("khaosnexus.card.forge_core", l.core()));
            }
            g.drawString(this.font, cost, x + 102, row + 1, l.unlocked() ? 0xFFD0C0E0 : 0xFF6A5A7A, false);
            row += 12;
        }
    }

    private void gateList(GuiGraphics g, int x, int y) {
        g.drawString(this.font, Component.translatable("khaosnexus.card.gates_header", this.gates.size(), this.radius), x, y, 0xFFA898B8, false);
        if (this.gates.isEmpty()) {
            g.drawString(this.font, Component.translatable("khaosnexus.card.no_gates"), x, y + 16, 0xFF8A7A9A, false);
            return;
        }
        int row = y + 16;
        for (int i = this.scroll; i < Math.min(this.gates.size(), this.scroll + 7); i++) {
            CardPayload.Gate gate = this.gates.get(i);
            GateRank r = GateRank.byName(gate.rank());
            int color = 0xFF000000 | r.color();
            g.fill(x, row, x + 18, row + 10, 0xFF1A1024);
            g.drawCenteredString(this.font, gate.rank(), x + 9, row + 1, color);
            g.drawString(this.font, Component.literal(gate.pos().getX() + ", " + gate.pos().getY() + ", " + gate.pos().getZ()), x + 26, row + 1, 0xFFF2EEF8, false);
            g.drawString(this.font, Component.translatable("khaosnexus.card.distance", gate.distance()), x + 150, row + 1, 0xFFD0C0E0, false);
            g.drawString(this.font, Component.translatable("khaosnexus.card.state." + gate.state()), x + 210, row + 1,
                    gate.state() == 2 ? 0xFF7A8A7A : gate.state() == 1 ? 0xFFFF6070 : 0xFFB44BE6, false);
            g.drawString(this.font, Component.translatable(gate.theme()), x + 26, row + 11, 0xFF9C8CB0, false);
            row += 23;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Slider extends AbstractSliderButton {
        private final String key;
        private final ModConfigSpec.DoubleValue config;
        private final double min;
        private final double max;
        private final boolean percent;

        Slider(int x, int y, String key, ModConfigSpec.DoubleValue config, double min, double max, boolean percent) {
            super(x, y, 200, 20, Component.empty(), (config.get() - min) / (max - min));
            this.key = key;
            this.config = config;
            this.min = min;
            this.max = max;
            this.percent = percent;
            this.updateMessage();
        }

        private double current() {
            return this.min + (this.max - this.min) * this.value;
        }

        @Override
        protected void updateMessage() {
            double v = this.current();
            this.setMessage(Component.translatable(this.key, this.percent ? Math.round(v * 100) + "%" : String.format("%.2fx", v)));
        }

        @Override
        protected void applyValue() {
            this.config.set(Math.round(this.current() * 100) / 100.0D);
            this.config.save();
        }
    }

    static final String[] GUILD_BUTTONS = {"spire_enter", "spire_leave", "spire_status", "spire_checkpoint", "top", "weekly",
            "prestige", "mastery", "aura", "title", "party", "bounty", "class"};

    /** Guild tab: one button per hunter command, two columns. Class opens the class screen; prestige needs a second click. */
    private void guildWidgets(float k) {
        int bw = 140, bh = 20, gap = 4;
        int sx = this.width / 2 - bw - gap / 2;
        int sy = (int) ((top() + 30) * k);
        for (int i = 0; i < GUILD_BUTTONS.length; i++) {
            String id = GUILD_BUTTONS[i];
            boolean armed = id.equals("prestige") && this.prestigeArmed;
            Button b = Button.builder(Component.translatable(armed ? "khaosnexus.card.guild.prestige_confirm" : "khaosnexus.card.guild." + id), btn -> {
                if (id.equals("class")) {
                    this.minecraft.setScreen(new ClassScreen());
                    return;
                }
                if (id.equals("prestige") && !this.prestigeArmed) {
                    this.prestigeArmed = true;
                    this.init();
                    return;
                }
                this.prestigeArmed = false;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.khaoskrew.khaosnexus.network.ActionPayload(com.khaoskrew.khaosnexus.network.ActionPayload.GUILD, id));
                if (id.startsWith("spire_enter") || id.startsWith("spire_leave")) {
                    this.onClose();
                }
            }).bounds(sx + (i % 2) * (bw + gap), sy + (i / 2) * (bh + gap), bw, bh).build();
            this.addRenderableWidget(b);
        }
    }
}
