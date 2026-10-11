package jp.pakupaku.tntbow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TntBowCommand implements CommandExecutor, TabCompleter {
    private final TntBowPlugin plugin;
    private final TntBowService bowService;

    public TntBowCommand(TntBowPlugin plugin, TntBowService bowService) {
        this.plugin = plugin;
        this.bowService = bowService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, label, args);
            case "reload" -> reload(sender);
            default -> {
                sendHelp(sender, label);
                yield true;
            }
        };
    }

    private boolean give(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("tntbow.give")) {
            error(sender, "このコマンドを使う権限がありません");
            return true;
        }
        if (args.length < 2) {
            sendGiveUsage(sender, label);
            return true;
        }

        Player target;
        int levelIndex;
        Integer firstNumber = parseInteger(args[1]);
        if (sender instanceof Player player && firstNumber != null) {
            target = player;
            levelIndex = 1;
        } else {
            if (args.length < 3) {
                sendGiveUsage(sender, label);
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                error(sender, "プレイヤー「" + args[1] + "」はオンラインではありません");
                return true;
            }
            levelIndex = 2;
        }

        Integer level = parseInteger(args[levelIndex]);
        if (level == null || !TntBowService.isValidLevel(level)) {
            error(sender, "レベルは1～5で指定してください");
            return true;
        }

        int amount = 1;
        if (args.length > levelIndex + 1) {
            Integer parsedAmount = parseInteger(args[levelIndex + 1]);
            if (parsedAmount == null || parsedAmount < 1 || parsedAmount > 64) {
                error(sender, "個数は1～64で指定してください");
                return true;
            }
            amount = parsedAmount;
        }

        ItemStack arrow = bowService.createArrow(level);
        arrow.setAmount(amount);
        Map<Integer, ItemStack> leftovers = target.getInventory().addItem(arrow);
        leftovers.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));

        sender.sendMessage(Component.text(target.getName() + " に TNT Arrow Lv." + level
                + " を " + amount + " 個渡しました", NamedTextColor.GREEN));
        if (!sender.equals(target)) {
            target.sendMessage(Component.text("TNT Arrow Lv." + level + " を受け取りました", NamedTextColor.GOLD));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("tntbow.reload")) {
            error(sender, "このコマンドを使う権限がありません");
            return true;
        }
        plugin.reloadConfig();
        bowService.reload();
        sender.sendMessage(Component.text("TNTBowの設定を再読み込みしました", NamedTextColor.GREEN));
        return true;
    }

    private static void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("TNT矢 コマンド", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/" + label + " give <レベル> [個数]", NamedTextColor.YELLOW)
                .append(Component.text(" - TNT矢を自分に渡す", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " give <プレイヤー> <レベル> [個数]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.YELLOW));
    }

    private static void sendGiveUsage(CommandSender sender, String label) {
        error(sender, "使い方: /" + label + " give [プレイヤー] <レベル1～5> [個数]");
    }

    private static void error(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private static @Nullable Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("tntbow.give")) {
                candidates.add("give");
            }
            if (sender.hasPermission("tntbow.reload")) {
                candidates.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (sender instanceof Player) {
                candidates.addAll(List.of("1", "2", "3", "4", "5"));
            }
            Bukkit.getOnlinePlayers().forEach(player -> candidates.add(player.getName()));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (parseInteger(args[1]) != null && sender instanceof Player) {
                candidates.addAll(List.of("1", "2", "4", "8", "16", "32", "64"));
            } else {
                candidates.addAll(List.of("1", "2", "3", "4", "5"));
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            candidates.addAll(List.of("1", "2", "4", "8", "16", "32", "64"));
        }

        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        candidates.removeIf(candidate -> !candidate.toLowerCase(Locale.ROOT).startsWith(current));
        Collections.sort(candidates);
        return candidates;
    }
}
