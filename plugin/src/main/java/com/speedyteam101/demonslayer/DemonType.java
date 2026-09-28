package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.EntityType;

/** Demon mobs. Bosses are the Twelve Kizuki and Muzan. */
public enum DemonType {
    LESSER("Demon", null, EntityType.ZOMBIE, 40, 6, 0.27, 1, 5, null),
    SWAMP("Swamp Demon", null, EntityType.ZOMBIE, 70, 7, 0.28, 3, 5, null),
    TEMARI("Temari Demon", null, EntityType.HUSK, 80, 6, 0.26, 3, 4, null),
    DRUM("Drum Demon", null, EntityType.ZOMBIE, 100, 7, 0.24, 4, 5, null),
    HAND("Hand Demon", null, EntityType.ZOMBIE, 140, 9, 0.2, 5, 5, null),

    RUI("Rui", "Lower Moon Five", EntityType.SKELETON, 300, 9, 0.3, 25, 3, BarColor.RED),
    ENMU("Enmu", "Lower Moon One", EntityType.ILLUSIONER, 350, 8, 0.3, 35, 3, BarColor.PURPLE),
    AKAZA("Akaza", "Upper Moon Three", EntityType.ZOMBIE, 600, 16, 0.34, 60, 3, BarColor.BLUE),
    DOMA("Doma", "Upper Moon Two", EntityType.STRAY, 650, 14, 0.3, 70, 3, BarColor.WHITE),
    KOKUSHIBO("Kokushibo", "Upper Moon One", EntityType.WITHER_SKELETON, 800, 20, 0.32, 90, 3, BarColor.PURPLE),
    MUZAN("Muzan Kibutsuji", "Demon King", EntityType.VINDICATOR, 1000, 22, 0.35, 150, 3, BarColor.RED);

    private final String name;
    private final String title;
    private final EntityType entity;
    private final double health;
    private final double damage;
    private final double speed;
    private final int merit;
    /** Seconds between special attacks. */
    private final int cooldown;
    private final BarColor barColor;

    DemonType(String name, String title, EntityType entity, double health, double damage, double speed,
              int merit, int cooldown, BarColor barColor) {
        this.name = name;
        this.title = title;
        this.entity = entity;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.merit = merit;
        this.cooldown = cooldown;
        this.barColor = barColor;
    }

    public boolean isBoss() {
        return title != null;
    }

    public String title() {
        return title;
    }

    public String fullName() {
        return isBoss() ? title + " " + name : name;
    }

    public Component displayName() {
        if (isBoss()) {
            return Component.text(title + " ", NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD)
                    .append(Component.text(name, NamedTextColor.RED).decoration(TextDecoration.BOLD, false));
        }
        return Component.text(name, NamedTextColor.RED);
    }

    public EntityType entity() {
        return entity;
    }

    public double health() {
        return health;
    }

    public double damage() {
        return damage;
    }

    public double speed() {
        return speed;
    }

    public int merit() {
        return merit;
    }

    public int cooldown() {
        return cooldown;
    }

    public BarColor barColor() {
        return barColor;
    }

    public static DemonType parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
