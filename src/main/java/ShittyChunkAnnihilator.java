import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static org.bukkit.Material.*;

import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ShittyChunkAnnihilator implements Listener {

    final private Plugin plugin;
    final private ConcurrentLinkedQueue<LinkedList<Block>> queue;

    public ShittyChunkAnnihilator(Plugin plugin, ConcurrentLinkedQueue<LinkedList<Block>> queue) {
        this.plugin = plugin;
        this.queue = queue;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    var chunk = event.getChunk();
                    var world = chunk.getWorld();
                    var x = chunk.getX() * 16;
                    var z = chunk.getZ() * 16;

                    final var preservedBlocks = Set.of(
                            BLACK_BED, // (Partial) villages and possibly villagers
                            BLUE_BED,
                            BROWN_BED,
                            CYAN_BED,
                            GRAY_BED,
                            GREEN_BED,
                            LIGHT_BLUE_BED,
                            LIGHT_GRAY_BED,
                            LIME_BED,
                            MAGENTA_BED,
                            ORANGE_BED,
                            PINK_BED,
                            PURPLE_BED,
                            RED_BED,
                            WHITE_BED,
                            YELLOW_BED,
                            CHEST, // (Partial) ruined portals, (partial) villages, hidden treasure
                            CHEST_MINECART, // Mineshafts
                            END_PORTAL_FRAME,
                            NETHER_PORTAL, // Ensure the players don't immediately fall to their deaths in the nether
                            NETHER_BRICK, // Nether fortresses (contain blaze spawners!)
                            SPAWNER
                    );

                    // Link the deleted chunks to the world seed; make sure the random number generator actually
                    // has valid seeds by bijectively pairing x, z to a number that is used as part of the random seed.
                    // This might be a complete brainfart, but after two beers and a quick "I HAVE THIS PROBLEM AND IT
                    // IS VAGUELY MATHEMATICAL IN NATURE, HELP!" session with my actuary wife this seemed like the best
                    // way to ensure we delete the same chunks without having weird clusters around (0,0).
                    // If you have a better way of doing this, by all means, let me know :)
                    //
                    // Edited to add: Chunk.getChunkKey() would be fine here instead. Leaving the previous method
                    // because we wanted to preserve previously found world seeds that would have been invalidated by
                    // using new Math.random seeds during chunk removal. The performance impact of this more complicated
                    // way of ensuring uniqueness is negligible since it happens just once per chunk.
                    long cantorAdjuster;
                    if (x >= 0 && z >= 0) {
                        cantorAdjuster = 0;
                    } else if (x < 0 && z >= 0) {
                        cantorAdjuster = 1;
                    } else if (x < 0 /* implicit: && z < 0*/) {
                        cantorAdjuster = 2;
                    } else {
                        cantorAdjuster = 3;
                    }

                    var absX = Math.abs(x);
                    var absZ = Math.abs(z);
                    var cantorPairing = (long) (0.5 * (absX + absZ) * (absX + absZ + 1) + absZ);
                    var random = new Random(world.getSeed() + (cantorPairing * 4 + cantorAdjuster));

                    if (world.getName().endsWith("_the_end")) {
                        // Don't change the end
                        return;
                    }

                    var spawnBlock = world.getBlockAt(world.getSpawnLocation());
                    var spawnChunk = spawnBlock.getChunk();

                    if (chunk.getChunkKey() == spawnChunk.getChunkKey()) {  // FIXME: Check whether this actually works!
                        for (var player: Bukkit.getServer().getOnlinePlayers()) {
                            // For some seed and paper versions, the player can spawn in a chunk next to the
                            // spawn chunk; teleport them to the actual spawn chunk in order to make sure they don't
                            // die
                            player.teleport(spawnBlock.getLocation());
                        }

                        return;
                    }

                    var startTime = System.currentTimeMillis();
                    if (random.nextFloat() < 0.99) {
                        // Might be faster, depending on how getHighestBlockAt() is implemented:
                        // h = world.getHighestBlockAt()
                        var toDelete = new LinkedList<Block>();
                        for (int h = 254; h >= 0; h--) {
                            for (int i = 0; i < 16; i++) {
                                for (int j = 0; j < 16; j++) {
                                    var block = world.getBlockAt(x + i, h, z + j);
                                    var blockType = block.getType();

                                    if (preservedBlocks.contains(blockType)) {
                                        // Keep this whole chunk; there's something of interest here!
                                        return;
                                    }

                                    if (blockType != AIR) {
                                        toDelete.add(block);
                                    }
                                }
                            }
                        }

                        queue.add(toDelete);
                    }

                    Bukkit.getServer().getConsoleSender().sendMessage("ShittySkyChunk Annihilator enqueue took " + (System.currentTimeMillis() - startTime) + "ms");
                }
            };

            runnable.runTask(plugin);
        }
    }
}