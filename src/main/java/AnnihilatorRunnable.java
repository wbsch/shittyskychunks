import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.bukkit.Material.AIR;

public class AnnihilatorRunnable extends BukkitRunnable {
    private final Plugin plugin;
    private final ConcurrentLinkedQueue<LinkedList<Block>> queue;

    public AnnihilatorRunnable(Plugin plugin, ConcurrentLinkedQueue<LinkedList<Block>> queue) {
        this.plugin = plugin;
        this.queue = queue;
    }

    @Override
    public void run() {
        var toDelete = queue.poll();

        if (toDelete != null) {
            var startTime = System.currentTimeMillis();
            for (var block : toDelete) {
                // We want to keep the "updatePhysics" parameter to its default value of "true" here in order for
                // already flowing water/lava to be deleted/to stop flowing at some point when its source block
                // is deleted.
                // NOTE: This sometimes leads to "lava chutes" where water and lava were flowing being left behind!
                //
                // The reason we still set the physics updates to "false" instead is that, since we're removing whole
                // chunks, removing one leads to all chunks that this one touches to be loaded and updated as well
                // because of the block updates at chunk borders.
                //
                // A possible workaround would be either checking for border blocks here, or having two lists - one
                // with nothing but border blocks, and one for all others. Those lists would be filled during initial
                // queuing and lead to less overhead.
                block.setType(AIR, false);
            }

            // Bukkit.getServer().getConsoleSender().sendMessage("ShittySkyChunk Annihilator deletion took " + (System.currentTimeMillis() - startTime) + "ms, queue length: " + queue.size());
        }

    }
}
