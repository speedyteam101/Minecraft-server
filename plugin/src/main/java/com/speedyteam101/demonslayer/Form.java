package com.speedyteam101.demonslayer;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/** One technique of a style: its name, breath cost and effect. */
public record Form(String name, int cost, Consumer<Player> action) {
}
