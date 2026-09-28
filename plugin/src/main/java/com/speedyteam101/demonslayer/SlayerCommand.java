package com.speedyteam101.demonslayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /slayer command. */
public final class SlayerCommand implements TabExecutor {
    private static final String ADMIN = "demonslayer.admin";
    private static final List<String> PLAYER_SUBS = List.of("help", "menu", "style", "info");
    private static final List<String> ADMIN_SUBS = List.of("give", "summon", "demon", "human", "merit");

    private final DemonSlayerPlugin plugin;

    public SlayerCommand(DemonSlayerPlugin plugin) {
        this.plugin = plugin;
    }

    private static void msg(CommandSender to, String text, NamedTextColor color) {
        to.sendMessage(Component.text(text, color));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (ADMIN_SUBS.contains(sub) && !sender.hasPermission(ADMIN)) {
            msg(sender, "You don't have permission to do that.", NamedTextColor.RED);
            return true;
        }
        switch (sub) {
            case "menu" -> {
                if (sender instanceof Player p) {
                    StyleMenu.open(plugin, p);
                } else {
                    msg(sender, "Only players can open the menu.", NamedTextColor.RED);
                }
            }
            case "style" -> style(sender, args);
            case "info" -> info(sender, args);
            case "give" -> give(sender, args);
            case "summon" -> summon(sender, args);
            case "demon", "human" -> demon(sender, args, sub.equals("demon"));
            case "merit" -> merit(sender, args);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender s, String label) {
        msg(s, "--- Demon Slayer ---", NamedTextColor.GOLD);
        msg(s, "/" + label + " menu - choose a breathing style (or Blood Demon Art)", NamedTextColor.YELLOW);
        msg(s, "/" + label + " style <name> - choose a style by name", NamedTextColor.YELLOW);
        msg(s, "/" + label + " info [player] - rank, merit and style", NamedTextColor.YELLOW);
        msg(s, "Right-click with a Nichirin blade: use form. Sneak + right-click: switch form. F also works.", NamedTextColor.GRAY);
        if (s.hasPermission(ADMIN)) {
            msg(s, "/" + label + " give <" + String.join("|", Items.ALL) + "> [player]", NamedTextColor.RED);
            msg(s, "/" + label + " summon <demon> - " + names(DemonType.values()), NamedTextColor.RED);
            msg(s, "/" + label + " demon|human <player> - turn a player into a demon or back", NamedTextColor.RED);
            msg(s, "/" + label + " merit <player> <amount> - add merit (negative to remove)", NamedTextColor.RED);
        }
    }

    private void style(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            msg(sender, "Only players can choose a style.", NamedTextColor.RED);
            return;
        }
        Style s = args.length > 1 ? Style.parse(String.join("_", Arrays.copyOfRange(args, 1, args.length))) : null;
        if (s == null) {
            msg(p, "Unknown style. Styles: " + names(Style.values()), NamedTextColor.RED);
            return;
        }
        plugin.slayers().learn(p, s);
    }

    private void info(CommandSender sender, String[] args) {
        Player p = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player self ? self : null);
        if (p == null) {
            msg(sender, "Player not found.", NamedTextColor.RED);
            return;
        }
        Slayers s = plugin.slayers();
        Rank rank = s.rank(p);
        Style style = s.style(p);
        msg(sender, "--- " + p.getName() + " ---", NamedTextColor.GOLD);
        msg(sender, "Status: " + (s.isDemon(p) ? "Demon" : "Demon Slayer"), NamedTextColor.YELLOW);
        msg(sender, "Rank: " + rank.displayName() + " (x" + String.format(Locale.ROOT, "%.1f", rank.power()) + " technique damage)", NamedTextColor.YELLOW);
        Rank next = rank.next();
        msg(sender, "Merit: " + s.merit(p) + (next == null ? "" : " / " + next.merit() + " for " + next.displayName()), NamedTextColor.YELLOW);
        msg(sender, "Style: " + (style == null ? "none (/slayer menu)" : style.displayName()), NamedTextColor.YELLOW);
    }

    private Player target(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Player p = Bukkit.getPlayerExact(args[index]);
            if (p == null) {
                msg(sender, "Player not found: " + args[index], NamedTextColor.RED);
            }
            return p;
        }
        if (sender instanceof Player p) {
            return p;
        }
        msg(sender, "Say which player.", NamedTextColor.RED);
        return null;
    }

    private void give(CommandSender sender, String[] args) {
        ItemStack item = args.length > 1 ? Items.create(args[1].toLowerCase(Locale.ROOT)) : null;
        if (item == null) {
            msg(sender, "Items: " + String.join(", ", Items.ALL), NamedTextColor.RED);
            return;
        }
        Player p = target(sender, args, 2);
        if (p == null) {
            return;
        }
        if (Items.isNichirin(item)) {
            Items.recolor(item, plugin.slayers().style(p));
        }
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItem(p.getLocation(), left));
        msg(sender, "Gave " + args[1] + " to " + p.getName() + ".", NamedTextColor.GREEN);
    }

    private void summon(CommandSender sender, String[] args) {
        DemonType type = args.length > 1 ? DemonType.parse(args[1]) : null;
        if (type == null) {
            msg(sender, "Demons: " + names(DemonType.values()), NamedTextColor.RED);
            return;
        }
        Location at;
        if (sender instanceof Player p) {
            org.bukkit.util.Vector forward = p.getLocation().getDirection().setY(0);
            at = forward.lengthSquared() < 1.0E-4 ? p.getLocation() : p.getLocation().add(forward.normalize().multiply(4));
        } else {
            at = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        plugin.demons().spawn(type, at);
        msg(sender, "Summoned " + type.fullName() + ".", NamedTextColor.GREEN);
    }

    private void demon(CommandSender sender, String[] args, boolean demon) {
        Player p = target(sender, args, 1);
        if (p == null) {
            return;
        }
        if (plugin.slayers().isDemon(p) == demon) {
            msg(sender, p.getName() + " is already " + (demon ? "a demon." : "human."), NamedTextColor.RED);
            return;
        }
        plugin.slayers().setDemon(p, demon);
        msg(sender, p.getName() + " is now " + (demon ? "a demon." : "human."), NamedTextColor.GREEN);
    }

    private void merit(CommandSender sender, String[] args) {
        Player p = target(sender, args, 1);
        if (p == null || args.length < 3) {
            msg(sender, "Usage: /slayer merit <player> <amount>", NamedTextColor.RED);
            return;
        }
        try {
            plugin.slayers().addMerit(p, Integer.parseInt(args[2]));
            msg(sender, p.getName() + " now has " + plugin.slayers().merit(p) + " merit.", NamedTextColor.GREEN);
        } catch (NumberFormatException e) {
            msg(sender, "Not a number: " + args[2], NamedTextColor.RED);
        }
    }

    private static String names(Enum<?>[] values) {
        List<String> out = new ArrayList<>();
        for (Enum<?> v : values) {
            out.add(v.name().toLowerCase(Locale.ROOT));
        }
        return String.join(", ", out);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(PLAYER_SUBS);
            if (sender.hasPermission(ADMIN)) {
                options.addAll(ADMIN_SUBS);
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "style" -> Arrays.stream(Style.values()).forEach(s -> options.add(s.name().toLowerCase(Locale.ROOT)));
                case "give" -> options.addAll(Items.ALL);
                case "summon" -> Arrays.stream(DemonType.values()).forEach(t -> options.add(t.name().toLowerCase(Locale.ROOT)));
                case "info", "demon", "human", "merit" -> Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                default -> {
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(o -> !o.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }
}
