package com.speedyteam101.demonslayer;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;

/** A particle together with the extra data some particles need. */
public record Fx(Particle particle, Object data) {

    public static Fx of(Particle particle) {
        return new Fx(particle, null);
    }

    public static Fx dust(Color color, float size) {
        return new Fx(Particle.REDSTONE, new Particle.DustOptions(color, size));
    }

    public static Fx block(Material material) {
        return new Fx(Particle.BLOCK_CRACK, material.createBlockData());
    }

    public void spawn(Location at, int count, double spread) {
        if (at.getWorld() == null) {
            return;
        }
        if (data == null) {
            at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, 0);
        } else {
            at.getWorld().spawnParticle(particle, at, count, spread, spread, spread, 0, data);
        }
    }
}
