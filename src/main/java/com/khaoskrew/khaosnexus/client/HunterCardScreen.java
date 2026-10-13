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
    private int tab;
    private int scroll;

    public HunterCardScreen(CardPayload payload) {
        super(Component.translatable("khaosnexus.card.title"));
        this.gates = payload.gates();
        this.radius = payload.radius();
        this.forge = payload.forge();
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
        String[] tabs = {"khaosnexus.card.tab.info", "khaosnexus.card.tab.gates", "khaosnexus.card.tab.settings", "khaosnexus.card.tab.forge"};
        for (int i = 0; i < tabs.length; i++) {
            int t = i;
            Button b = Button.builder(Component.translatable(tabs[i]), btn -> {
                this.tab = t;
                lastTab = t;
                this.scroll = 0;
                this.init();
            }).bounds(x + i * 75, y, 73, 18).build();
            b.active = i != this.tab;
            this.addRenderableWidget(b);
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
        line(g, x, y, Component.translatable("khaosnexus.card.rank"), Component.translatable(rank.translationKey()));
        line(g, x, y + 12, Component.translatable("khaosnexus.card.level"),
                Component.translatable("khaosnexus.card.level_value", rank.ordinal() + 1, p.xp, next == null ? "-" : String.valueOf(hi)));
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
                Component.translatable("khaosnexus.card.spire_value", p.spireFloor, p.spireBest));
        line(g, x, y + 150, Component.translatable("khaosnexus.card.party"), Component.literal(String.valueOf(p.partySize)));
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
            g.drawString(this.font, Component.literal("+" + l.level()), x + 70, row + 1, 0xFFB44BE6, false);
            GateRank r = GateRank.byName(l.rank());
            g.fill(x + 92, row, x + 110, row + 10, 0xFF1A1024);
            g.drawCenteredString(this.font, l.rank(), x + 101, row + 1, l.unlocked() ? 0xFF000000 | r.color() : 0xFF5A4A6A);
            Component cost = Component.translatable("khaosnexus.card.forge_cost", l.count(), l.tier(), l.essence());
            if (l.core() > 0) {
                cost = cost.copy().append(Component.translatable("khaosnexus.forge.cost_core", l.core()));
            }
            g.drawString(this.font, cost, x + 118, row + 1, l.unlocked() ? 0xFFD0C0E0 : 0xFF6A5A7A, false);
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
        for (int i = this.scroll; i < Math.min(this.gates.size(), this.scroll + 12); i++) {
            CardPayload.Gate gate = this.gates.get(i);
            GateRank r = GateRank.byName(gate.rank());
            int color = 0xFF000000 | r.color();
            g.fill(x, row, x + 18, row + 10, 0xFF1A1024);
            g.drawCenteredString(this.font, gate.rank(), x + 9, row + 1, color);
            g.drawString(this.font, Component.literal(gate.pos().getX() + ", " + gate.pos().getY() + ", " + gate.pos().getZ()), x + 26, row + 1, 0xFFF2EEF8, false);
            g.drawString(this.font, Component.translatable("khaosnexus.card.distance", gate.distance()), x + 150, row + 1, 0xFFD0C0E0, false);
            g.drawString(this.font, Component.translatable("khaosnexus.card.state." + gate.state()), x + 210, row + 1,
                    gate.state() == 2 ? 0xFF7A8A7A : gate.state() == 1 ? 0xFFFF6070 : 0xFFB44BE6, false);
            row += 12;
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
}
