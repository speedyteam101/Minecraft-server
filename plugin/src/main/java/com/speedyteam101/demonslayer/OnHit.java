package com.speedyteam101.demonslayer;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/** Extra effect applied to each target a technique hits. */
@FunctionalInterface
public interface OnHit {
    void apply(LivingEntity caster, LivingEntity target);

    default OnHit and(OnHit other) {
        return (c, t) -> {
            apply(c, t);
            other.apply(c, t);
        };
    }

    static OnHit none() {
        return (c, t) -> {
        };
    }

    static OnHit fire(int ticks) {
        return (c, t) -> t.setFireTicks(Math.max(t.getFireTicks(), ticks));
    }

    static OnHit effect(PotionEffectType type, int ticks, int amplifier) {
        return (c, t) -> t.addPotionEffect(new PotionEffect(type, ticks, amplifier));
    }

    static OnHit knockUp(double y) {
        return (c, t) -> t.setVelocity(t.getVelocity().setY(y));
    }

    static OnHit knockBack(double strength) {
        return (c, t) -> {
            Vector away = t.getLocation().toVector().subtract(c.getLocation().toVector()).setY(0);
            if (away.lengthSquared() < 1.0E-4) {
                return;
            }
            t.setVelocity(away.normalize().multiply(strength).setY(0.35));
        };
    }

    /** Pulls the target towards the caster. */
    static OnHit pull(double strength) {
        return (c, t) -> {
            Vector to = c.getLocation().toVector().subtract(t.getLocation().toVector()).setY(0);
            if (to.lengthSquared() < 1.0E-4) {
                return;
            }
            t.setVelocity(to.normalize().multiply(strength).setY(0.25));
        };
    }

    /** Harmless lightning flash and thunder at the target. */
    static OnHit lightning() {
        return (c, t) -> t.getWorld().strikeLightningEffect(t.getLocation());
    }

    /** Powder-snow style freezing. */
    static OnHit freeze(int ticks) {
        return (c, t) -> t.setFreezeTicks(Math.max(t.getFreezeTicks(), ticks));
    }
}
