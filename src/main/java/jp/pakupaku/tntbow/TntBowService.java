package jp.pakupaku.tntbow;

import java.util.List;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class TntBowService {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 5;

    private final TntBowPlugin plugin;
    private final float[] explosionPowers = new float[MAX_LEVEL + 1];
    private boolean breakBlocks;
    private boolean setFire;
    private int cooldownTicks;

    public TntBowService(TntBowPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
            double configured = config.getDouble("levels." + level + ".explosion-power", level * 2.0);
            if (!Double.isFinite(configured) || configured <= 0.0 || configured > 100.0) {
                plugin.getLogger().warning("levels." + level + ".explosion-power が範囲外のため "
                        + (level * 2.0) + " を使用します");
                configured = level * 2.0;
            }
            explosionPowers[level] = (float) configured;
        }

        breakBlocks = config.getBoolean("explosion.break-blocks", true);
        setFire = config.getBoolean("explosion.set-fire", false);
        cooldownTicks = Math.max(0, config.getInt("arrow.cooldown-ticks", 10));
    }

    public ItemStack createArrow(int level) {
        requireValidLevel(level);

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        NamedTextColor color = colorFor(level);
        meta.displayName(plain(Component.text("TNT Arrow Lv." + level, color).decorate(TextDecoration.BOLD)));
        meta.lore(List.of(
                plain(Component.text("通常の弓で発射できる特殊な矢", NamedTextColor.GRAY)),
                plain(Component.text("着弾地点で爆発します", NamedTextColor.GRAY)),
                plain(Component.text("爆発威力: " + explosionPowers[level], NamedTextColor.GOLD)),
                plain(Component.text("通常TNTの威力: 4.0", NamedTextColor.DARK_GRAY))));
        meta.getPersistentDataContainer().set(plugin.arrowItemLevelKey(), PersistentDataType.INTEGER, level);
        meta.setEnchantmentGlintOverride(true);
        arrow.setItemMeta(meta);
        return arrow;
    }

    public OptionalInt levelOf(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW || !item.hasItemMeta()) {
            return OptionalInt.empty();
        }
        Integer level = item.getItemMeta().getPersistentDataContainer()
                .get(plugin.arrowItemLevelKey(), PersistentDataType.INTEGER);
        return level != null && isValidLevel(level) ? OptionalInt.of(level) : OptionalInt.empty();
    }

    public float explosionPower(int level) {
        requireValidLevel(level);
        return explosionPowers[level];
    }

    public boolean breakBlocks() {
        return breakBlocks;
    }

    public boolean setFire() {
        return setFire;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public static boolean isValidLevel(int level) {
        return level >= MIN_LEVEL && level <= MAX_LEVEL;
    }

    private static void requireValidLevel(int level) {
        if (!isValidLevel(level)) {
            throw new IllegalArgumentException("TNT矢のレベルは1～5である必要があります: " + level);
        }
    }

    private static NamedTextColor colorFor(int level) {
        return switch (level) {
            case 1 -> NamedTextColor.GREEN;
            case 2 -> NamedTextColor.AQUA;
            case 3 -> NamedTextColor.YELLOW;
            case 4 -> NamedTextColor.GOLD;
            case 5 -> NamedTextColor.RED;
            default -> NamedTextColor.WHITE;
        };
    }

    private static Component plain(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }
}
