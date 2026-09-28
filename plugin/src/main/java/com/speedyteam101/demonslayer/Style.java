package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;

/** Breathing styles for slayers and Blood Demon Arts for demon players. */
public enum Style {
    WATER("Water Breathing", NamedTextColor.AQUA, Material.HEART_OF_THE_SEA, "Blue", Kind.BREATHING, Sound.ENTITY_GENERIC_SPLASH),
    FLAME("Flame Breathing", NamedTextColor.RED, Material.BLAZE_POWDER, "Red", Kind.BREATHING, Sound.ITEM_FIRECHARGE_USE),
    THUNDER("Thunder Breathing", NamedTextColor.YELLOW, Material.GLOWSTONE_DUST, "Yellow", Kind.BREATHING, Sound.ENTITY_LIGHTNING_BOLT_IMPACT),
    WIND("Wind Breathing", NamedTextColor.GREEN, Material.FEATHER, "Green", Kind.BREATHING, Sound.ENTITY_PHANTOM_FLAP),
    STONE("Stone Breathing", NamedTextColor.GRAY, Material.COBBLESTONE, "Grey", Kind.BREATHING, Sound.BLOCK_STONE_BREAK),
    MIST("Mist Breathing", NamedTextColor.WHITE, Material.GHAST_TEAR, "White", Kind.BREATHING, Sound.ENTITY_PLAYER_ATTACK_SWEEP),
    BEAST("Beast Breathing", NamedTextColor.BLUE, Material.BONE, "Indigo", Kind.BREATHING, Sound.ENTITY_WOLF_GROWL),
    INSECT("Insect Breathing", NamedTextColor.LIGHT_PURPLE, Material.PURPLE_DYE, "Violet", Kind.BREATHING, Sound.ENTITY_BEE_STING),
    SERPENT("Serpent Breathing", NamedTextColor.DARK_PURPLE, Material.LEAD, "Purple", Kind.BREATHING, Sound.ENTITY_PLAYER_ATTACK_SWEEP),
    LOVE("Love Breathing", NamedTextColor.LIGHT_PURPLE, Material.PINK_TULIP, "Pink", Kind.BREATHING, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    SOUND("Sound Breathing", NamedTextColor.GOLD, Material.NOTE_BLOCK, "Gold", Kind.BREATHING, Sound.ENTITY_GENERIC_EXPLODE),
    FLOWER("Flower Breathing", NamedTextColor.LIGHT_PURPLE, Material.PINK_PETALS, "Pale Pink", Kind.BREATHING, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    SUN("Sun Breathing", NamedTextColor.GOLD, Material.SUNFLOWER, "Jet Black", Kind.SUN, Sound.ENTITY_BLAZE_SHOOT),

    MOON("Moon Breathing", NamedTextColor.DARK_PURPLE, Material.ENDER_EYE, null, Kind.DEMON, Sound.ENTITY_PLAYER_ATTACK_SWEEP),
    DESTRUCTIVE_DEATH("Destructive Death", NamedTextColor.DARK_AQUA, Material.COMPASS, null, Kind.DEMON, Sound.ENTITY_PLAYER_ATTACK_STRONG),
    CRYOKINESIS("Cryokinesis", NamedTextColor.AQUA, Material.PACKED_ICE, null, Kind.DEMON, Sound.BLOCK_GLASS_BREAK),
    BLOOD_THREADS("Blood Threads", NamedTextColor.DARK_RED, Material.STRING, null, Kind.DEMON, Sound.ENTITY_SPIDER_AMBIENT),
    EXPLODING_BLOOD("Exploding Blood", NamedTextColor.RED, Material.FIRE_CHARGE, null, Kind.DEMON, Sound.ENTITY_GENERIC_EXPLODE);

    public enum Kind {
        /** Any human slayer can learn it. */
        BREATHING,
        /** Human slayers of Hashira rank only. */
        SUN,
        /** Demon players only (Blood Demon Arts). */
        DEMON
    }

    private final String displayName;
    private final NamedTextColor color;
    private final Material icon;
    private final String bladeColor;
    private final Kind kind;
    private final Sound sound;

    Style(String displayName, NamedTextColor color, Material icon, String bladeColor, Kind kind, Sound sound) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.bladeColor = bladeColor;
        this.kind = kind;
        this.sound = sound;
    }

    public String displayName() {
        return displayName;
    }

    public NamedTextColor color() {
        return color;
    }

    public Material icon() {
        return icon;
    }

    /** Colour a Nichirin blade turns for this style, or null for demon arts. */
    public String bladeColor() {
        return bladeColor;
    }

    public Kind kind() {
        return kind;
    }

    public Sound sound() {
        return sound;
    }

    public boolean isDemonArt() {
        return kind == Kind.DEMON;
    }

    public static Style parse(String name) {
        if (name == null) {
            return null;
        }
        String n = name.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (Style s : values()) {
            if (s.name().equals(n) || s.displayName.toUpperCase().replace(' ', '_').equals(n)) {
                return s;
            }
        }
        return null;
    }
}
