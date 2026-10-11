package jp.kamei.tntbow;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class TntBowPlugin extends JavaPlugin {
    private NamespacedKey arrowItemLevelKey;
    private NamespacedKey arrowLevelKey;
    private NamespacedKey explodedKey;
    private TntBowService bowService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        arrowItemLevelKey = new NamespacedKey(this, "arrow_item_level");
        arrowLevelKey = new NamespacedKey(this, "arrow_level");
        explodedKey = new NamespacedKey(this, "exploded");

        bowService = new TntBowService(this);
        bowService.reload();

        getServer().getPluginManager().registerEvents(new TntBowListener(this, bowService), this);

        TntBowCommand commandHandler = new TntBowCommand(this, bowService);
        PluginCommand command = getCommand("tntbow");
        if (command == null) {
            throw new IllegalStateException("plugin.ymlにtntbowコマンドがありません");
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getLogger().info("TNTBow 1.0.0 を有効化しました（TNT矢・威力レベル1～5）");
    }

    public NamespacedKey arrowItemLevelKey() {
        return arrowItemLevelKey;
    }

    public NamespacedKey arrowLevelKey() {
        return arrowLevelKey;
    }

    public NamespacedKey explodedKey() {
        return explodedKey;
    }
}
