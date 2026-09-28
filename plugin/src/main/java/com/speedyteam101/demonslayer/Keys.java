package com.speedyteam101.demonslayer;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Persistent data keys shared by the whole plugin. */
public final class Keys {
    /** Custom item id on an ItemStack. */
    public static NamespacedKey ITEM;
    /** Player: chosen style name. */
    public static NamespacedKey STYLE;
    /** Player: selected form index. */
    public static NamespacedKey FORM;
    /** Player: merit points (decide the rank). */
    public static NamespacedKey MERIT;
    /** Player: 1 when the player is a demon. */
    public static NamespacedKey DEMON;
    /** Player: 1 once the starter kit was handed out. */
    public static NamespacedKey JOINED;
    /** Entity: demon type name. */
    public static NamespacedKey DEMON_TYPE;

    public static NamespacedKey RECIPE_NICHIRIN;
    public static NamespacedKey RECIPE_UNIFORM;
    public static NamespacedKey RECIPE_CURE;

    private Keys() {
    }

    static void init(Plugin plugin) {
        ITEM = new NamespacedKey(plugin, "item");
        STYLE = new NamespacedKey(plugin, "style");
        FORM = new NamespacedKey(plugin, "form");
        MERIT = new NamespacedKey(plugin, "merit");
        DEMON = new NamespacedKey(plugin, "demon");
        JOINED = new NamespacedKey(plugin, "joined");
        DEMON_TYPE = new NamespacedKey(plugin, "demon_type");
        RECIPE_NICHIRIN = new NamespacedKey(plugin, "nichirin_blade");
        RECIPE_UNIFORM = new NamespacedKey(plugin, "slayer_uniform");
        RECIPE_CURE = new NamespacedKey(plugin, "tamayo_cure");
    }
}
