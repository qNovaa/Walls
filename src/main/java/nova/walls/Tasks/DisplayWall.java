package nova.walls.Tasks;

import nova.walls.Models.Wall;
import nova.walls.Walls;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;

public class DisplayWall {

    private static final int OFFSET = 1; // how many blocks between the particles
    private static final long TIMER = 2; // how long it takes to move up RISE
    private static final double RISE = 0.2; // how many blocks to rise each time

    private static BukkitTask borderTask;

    private Walls plugin;

    public DisplayWall(Walls plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        Wall wall = Walls.gameController.getWall();

        final double[] o = {0};
        borderTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : wall.getCreatedBlocks()) {
                    if (Walls.gameController.getWall().getOrientation() == Wall.Orientation.NORTHSOUTH) {
                        spawnParticle(block.getLocation().add(o[0], o[0], 0.5));
                    } else {
                        spawnParticle(block.getLocation().add(0.5, o[0], o[0]));
                    }
                }

                for (Block block : wall.getDeadBlocks()) {
                    if (Walls.gameController.getWall().getOrientation() == Wall.Orientation.NORTHSOUTH) {
                        spawnParticle(block.getLocation().add(o[0], o[0], 0));
                        spawnParticle(block.getLocation().add(o[0], o[0], 1));
                    } else {
                        spawnParticle(block.getLocation().add(0, o[0], o[0]));
                        spawnParticle(block.getLocation().add(1, o[0], o[0]));
                    }
                }
                o[0] = (o[0] >= OFFSET) ? 0 : o[0] + RISE;
            }
        }.runTaskTimer(plugin, 0L, TIMER);
    }

    private void spawnParticle(Location loc) {
        Walls.world.spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0F));
    }

    public void cancel() {
        if (borderTask != null) {
            borderTask.cancel();
            borderTask = null;
        }
    }
}
