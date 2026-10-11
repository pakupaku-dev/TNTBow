package jp.pakupaku.tntbow;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

public final class TntBowListener implements Listener {
    private final TntBowPlugin plugin;
    private final TntBowService bowService;
    private final Map<UUID, Long> nextShotNanos = new HashMap<>();

    public TntBowListener(TntBowPlugin plugin, TntBowService bowService) {
        this.plugin = plugin;
        this.bowService = bowService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBowShoot(EntityShootBowEvent event) {
        OptionalInt levelResult = bowService.levelOf(event.getConsumable());
        if (levelResult.isEmpty()) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        if (!player.hasPermission("tntbow.use")) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("TNT矢を使う権限がありません", NamedTextColor.RED));
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            event.setCancelled(true);
            plugin.getLogger().warning("TNT矢から矢以外の発射物が生成されたため発射を中止しました");
            return;
        }

        int cooldownTicks = bowService.cooldownTicks();
        long now = System.nanoTime();
        long nextAllowed = nextShotNanos.getOrDefault(player.getUniqueId(), 0L);
        if (cooldownTicks > 0 && now < nextAllowed) {
            event.setCancelled(true);
            double seconds = (nextAllowed - now) / 1_000_000_000.0;
            player.sendActionBar(Component.text(String.format("再発射まで %.1f 秒", seconds), NamedTextColor.RED));
            return;
        }
        nextShotNanos.put(player.getUniqueId(), now + cooldownTicks * 50_000_000L);

        int level = levelResult.getAsInt();
        arrow.getPersistentDataContainer().set(plugin.arrowLevelKey(), PersistentDataType.INTEGER, level);
        arrow.setGlowing(true);
        arrow.customName(Component.text("TNT Arrow Lv." + level, NamedTextColor.RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_TNT_PRIMED,
                SoundCategory.PLAYERS, 0.65f, 1.25f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }

        Integer level = arrow.getPersistentDataContainer()
                .get(plugin.arrowLevelKey(), PersistentDataType.INTEGER);
        if (level == null || !TntBowService.isValidLevel(level)) {
            return;
        }
        if (arrow.getPersistentDataContainer().has(plugin.explodedKey(), PersistentDataType.BYTE)) {
            return;
        }
        arrow.getPersistentDataContainer().set(plugin.explodedKey(), PersistentDataType.BYTE, (byte) 1);

        Location impact = arrow.getLocation().clone();
        ProjectileSource shooter = arrow.getShooter();
        Entity source = shooter instanceof Entity entity ? entity : null;
        arrow.remove();

        impact.getWorld().spawnParticle(Particle.FLAME, impact, 25, 0.25, 0.25, 0.25, 0.03);
        impact.getWorld().spawnParticle(Particle.SMOKE, impact, 18, 0.3, 0.3, 0.3, 0.02);
        impact.getWorld().createExplosion(impact, bowService.explosionPower(level),
                bowService.setFire(), bowService.breakBlocks(), source);
    }
}
