package com.khaoskrew.khaosnexus.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;

public final class ClassKeybinds {
    public static final String CATEGORY = "key.categories.khaosnexus";
    public static final KeyMapping PRIMARY = new KeyMapping("key.khaosnexus.ability", GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping SECONDARY = new KeyMapping("key.khaosnexus.ability_alt", GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping CLASS_MENU = new KeyMapping("key.khaosnexus.class", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping OPEN_CARD = new KeyMapping("key.khaosnexus.card", GLFW.GLFW_KEY_J, CATEGORY);

    private ClassKeybinds() {}
}
