package com.speedyteam101.demonslayer;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Damage rules: who techniques may hit, Nichirin bonus damage, and demons that
 * can only be killed by a Nichirin blade or sunlight.
 */
public final class Combat implements Listener {
    private static final long NICHIRIN_MARK_MS = 5000;
    private static final Fx REGROW = Fx.dust(Color.fromRGB(120, 0, 0), 1.4f);
    /** Damage causes demons can't shrug off: the void, /kill and the world border (names differ across versions). */
    private static final java.util.Set<String> ALWAYS_LETHAL = java.util.Set.of("VOID", "KILL", "SUICIDE", "WORLD_BORDER");

    private final DemonSlayerPlugin plugin;
    private final Map<UUID, Long> nichirinMarks = new HashMap<>();
    private final Map<UUID, Long> noFall = new HashMap<>();
    /** True while a technique is dealing its damage. */
    private boolean dealing;
    /** True while sunlight or other "true" damage is dealt; skips demon immortality. */
    private boolean bypass;

    public Combat(DemonSlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isDemon(Entity e) {
        return plugin.demons().isDemonMob(e) || (e instanceof Player p && plugin.slayers().isDemon(p));
    }

    /** Whether a technique cast by {@code caster} may damage {@code target}. */
    public boolean canHit(LivingEntity caster, LivingEntity target) {
        if (target == caster || target.isDead() || target instanceof ArmorStand) {
            return false;
        }
        if (target instanceof Player tp) {
            if (tp.getGameMode() == GameMode.CREATIVE || tp.getGameMode() == GameMode.SPECTATOR) {
                return false;
            }
            if (caster instanceof Player && !target.getWorld().getPVP()) {
                return false;
            }
        }
        boolean casterDemon = isDemon(caster);
        boolean targetDemon = isDemon(target);
        if (casterDemon) {
            return !targetDemon;
        }
        if (targetDemon) {
            return true;
        }
        return !(target instanceof Player) && target instanceof Enemy;
    }

    /** Technique damage multiplier of the caster. */
    public double power(LivingEntity caster) {
        if (caster instanceof Player p) {
            double m = plugin.slayers().rank(p).power();
            if (plugin.slayers().isDemon(p) && !Demons.isDay(p.getWorld())) {
                m *= 1.2;
            }
            return m;
        }
        return 1.0;
    }

    /** Deals technique damage. */
    public void hit(LivingEntity caster, LivingEntity target, double damage) {
        double amount = damage * power(caster);
        if (!isDemon(caster) && isDemon(target)) {
            amount *= 1.5;
            markNichirin(target);
        }
        dealing = true;
        try {
            target.setNoDamageTicks(0);
            target.damage(amount, caster);
        } finally {
            dealing = false;
        }
    }

    /** Damage that ignores demon immortality (sunlight). */
    public void trueDamage(LivingEntity target, double amount) {
        bypass = true;
        try {
            target.damage(amount);
        } finally {
            bypass = false;
        }
    }

    public void markNichirin(LivingEntity demon) {
        nichirinMarks.put(demon.getUniqueId(), System.currentTimeMillis() + NICHIRIN_MARK_MS);
    }

    /** Hit by a Nichirin blade in the last few seconds: can die and does not regenerate. */
    public boolean isMarked(Entity demon) {
        Long until = nichirinMarks.get(demon.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public void forget(UUID id) {
        nichirinMarks.remove(id);
        noFall.remove(id);
    }

    public void noFall(LivingEntity e, int ticks) {
        noFall.put(e.getUniqueId(), System.currentTimeMillis() + ticks * 50L);
    }

    private static Player attacker(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            return p;
        }
        if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    private boolean nichirinMelee(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent byEntity) || dealing) {
            return false;
        }
        Player a = attacker(byEntity);
        return a != null && byEntity.getDamager() == a && !plugin.slayers().isDemon(a)
                && Items.isNichirin(a.getInventory().getItemInMainHand());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Long until = noFall.get(victim.getUniqueId());
            if (until != null && until > System.currentTimeMillis()) {
                e.setCancelled(true);
                return;
            }
        }
        if (bypass || ALWAYS_LETHAL.contains(e.getCause().name())) {
            return;
        }

        if (victim instanceof Player vp) {
            if (plugin.slayers().isDemon(vp) && nichirinMelee(e)) {
                e.setDamage(e.getDamage() * 1.5);
            }
            return;
        }
        if (!plugin.demons().isDemonMob(victim)) {
            return;
        }

        if (nichirinMelee(e)) {
            Player a = attacker((EntityDamageByEntityEvent) e);
            e.setDamage(e.getDamage() * 1.5 * plugin.slayers().rank(a).power());
            markNichirin(victim);
        }
        if (!isMarked(victim)) {
            e.setDamage(e.getDamage() * plugin.getConfig().getDouble("demons.non-nichirin-damage-multiplier", 0.35));
            if (victim.getHealth() - e.getFinalDamage() <= 0.5) {
                // A demon's body regrows: only a Nichirin blade or sunlight can finish it.
                e.setCancelled(true);
                REGROW.spawn(victim.getLocation().add(0, 1, 0), 12, 0.4);
            }
        }
    }
}
