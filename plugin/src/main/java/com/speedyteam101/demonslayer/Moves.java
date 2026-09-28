package com.speedyteam101.demonslayer;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Building blocks for breathing forms, Blood Demon Arts and boss attacks.
 * The caster can be a player or a mob; mobs aim at their current target.
 */
public final class Moves {
    private Moves() {
    }

    private static DemonSlayerPlugin plugin() {
        return DemonSlayerPlugin.get();
    }

    /** Direction the caster is attacking in. */
    public static Vector aim(LivingEntity caster) {
        if (caster instanceof Mob mob && mob.getTarget() != null && mob.getTarget().getWorld().equals(caster.getWorld())) {
            Vector to = mob.getTarget().getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
            if (to.lengthSquared() > 1.0E-4) {
                return to.normalize();
            }
        }
        return caster.getEyeLocation().getDirection();
    }

    /** Living entities the caster is allowed to hit within a radius of a point. */
    public static List<LivingEntity> targets(LivingEntity caster, Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        if (center.getWorld() == null) {
            return out;
        }
        Combat combat = plugin().combat();
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity t && combat.canHit(caster, t)
                    && t.getLocation().add(0, t.getHeight() / 2, 0).distanceSquared(center) <= radius * radius) {
                out.add(t);
            }
        }
        return out;
    }

    public static void hit(LivingEntity caster, LivingEntity target, double damage, OnHit onHit) {
        plugin().combat().hit(caster, target, damage);
        if (onHit != null && target.isValid() && !target.isDead()) {
            onHit.apply(caster, target);
        }
    }

    /** Slash in a cone in front of the caster. */
    public static void arc(LivingEntity caster, double range, double angle, double damage, Fx fx, OnHit onHit) {
        Location eye = caster.getEyeLocation();
        Vector dir = aim(caster);
        double half = Math.toRadians(angle / 2);
        for (double a = -half; a <= half; a += Math.toRadians(8)) {
            Vector v = dir.clone().rotateAroundY(a);
            for (double r = 1.5; r <= range; r += 1.0) {
                fx.spawn(eye.clone().add(v.clone().multiply(r)), 1, 0.05);
            }
        }
        for (LivingEntity t : targets(caster, eye, range + 0.5)) {
            Vector to = t.getLocation().add(0, t.getHeight() / 2, 0).toVector().subtract(eye.toVector());
            if (to.lengthSquared() < 1.0 || to.angle(dir) <= half + 0.25) {
                hit(caster, t, damage, onHit);
            }
        }
    }

    /** Hits everything in a sphere around a point. */
    public static void aoe(LivingEntity caster, Location center, double radius, double damage, Fx fx, OnHit onHit) {
        ring(center, radius, fx);
        ring(center, radius * 0.5, fx);
        for (LivingEntity t : targets(caster, center, radius)) {
            hit(caster, t, damage, onHit);
        }
    }

    public static void ring(Location center, double radius, Fx fx) {
        int points = Math.max(12, (int) (radius * 10));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            fx.spawn(center.clone().add(Math.cos(a) * radius, 0.3, Math.sin(a) * radius), 1, 0.05);
        }
    }

    /** A straight line attack that pierces through targets and stops at walls. */
    public static void beam(LivingEntity caster, double range, double width, double damage, Fx fx, OnHit onHit) {
        Location eye = caster.getEyeLocation();
        Vector dir = aim(caster);
        Set<UUID> hit = new HashSet<>();
        for (double d = 0.5; d <= range; d += 0.5) {
            Location p = eye.clone().add(dir.clone().multiply(d));
            if (p.getBlock().getType().isSolid()) {
                break;
            }
            fx.spawn(p, 2, 0.1);
            for (LivingEntity t : targets(caster, p, width)) {
                if (hit.add(t.getUniqueId())) {
                    hit(caster, t, damage, onHit);
                }
            }
        }
    }

    /** Dashes forward, hitting each target along the way once. */
    public static void dash(LivingEntity caster, double speed, int ticks, double damage, Fx fx, OnHit onHit) {
        Vector dir = aim(caster).clone();
        dir.setY(Math.max(-0.3, Math.min(0.35, dir.getY())));
        if (dir.lengthSquared() < 1.0E-4) {
            return;
        }
        dir.normalize();
        plugin().combat().noFall(caster, 60);
        Set<UUID> hit = new HashSet<>();
        new BukkitRunnable() {
            int t = 0;

            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead() || t++ >= ticks) {
                    cancel();
                    return;
                }
                caster.setVelocity(dir.clone().multiply(speed));
                Location at = caster.getLocation().add(0, 1, 0);
                fx.spawn(at, 4, 0.3);
                for (LivingEntity target : targets(caster, at, 2.3)) {
                    if (hit.add(target.getUniqueId())) {
                        hit(caster, target, damage, onHit);
                    }
                }
            }
        }.runTaskTimer(plugin(), 0L, 1L);
    }

    /** Jumps up and forward. */
    public static void leap(LivingEntity caster, double up, double forward) {
        Vector flat = aim(caster).clone().setY(0);
        if (flat.lengthSquared() > 1.0E-4) {
            flat.normalize().multiply(forward);
        }
        caster.setVelocity(flat.setY(up));
        plugin().combat().noFall(caster, 80);
    }

    /** Runs an action several times, stopping if the caster dies or leaves. */
    public static void repeat(LivingEntity caster, int times, int interval, Runnable action) {
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (!caster.isValid() || caster.isDead() || i++ >= times) {
                    cancel();
                    return;
                }
                action.run();
            }
        }.runTaskTimer(plugin(), 0L, interval);
    }

    public static void later(LivingEntity caster, int delay, Runnable action) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (caster.isValid() && !caster.isDead()) {
                    action.run();
                }
            }
        }.runTaskLater(plugin(), delay);
    }

    public static void buff(LivingEntity caster, PotionEffectType type, int ticks, int amplifier) {
        caster.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true));
    }

    public static void sound(Entity at, Sound sound, float volume, float pitch) {
        at.getWorld().playSound(at.getLocation(), sound, volume, pitch);
    }

    /** Teleports the caster behind the target if there is room. */
    public static void behind(LivingEntity caster, LivingEntity target) {
        Vector back = target.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-4) {
            back = new Vector(1, 0, 0);
        }
        Location to = target.getLocation().subtract(back.normalize().multiply(2));
        if (to.getBlock().isPassable() && to.clone().add(0, 1, 0).getBlock().isPassable()) {
            to.setDirection(target.getLocation().toVector().subtract(to.toVector()));
            caster.teleport(to);
        }
    }
}
