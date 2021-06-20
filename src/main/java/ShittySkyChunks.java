import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ShittySkyChunks extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("ShittySkyChunks loading...");

        var queue = new ConcurrentLinkedQueue<LinkedList<Block>>();
        getServer().getPluginManager().registerEvents(new ShittyChunkAnnihilator(this, queue), this);

        var runnable = new AnnihilatorRunnable(this, queue);
        runnable.runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }
}
