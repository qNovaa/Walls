package nova.walls.Events;

import nova.walls.Models.Wall;
import nova.walls.Walls;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BreakBorder implements Listener {

    @EventHandler
    public void onPlayerBreakBorder(BlockBreakEvent event) {

        // if block is part of wall is broken, cancel it
        Block blockBroken = event.getBlock();
        Wall wall = Walls.gameController.getWall();
        if (wall == null) return;
        if (wall.getCreatedBlocks().contains(blockBroken) || wall.getDeadBlocks().contains(blockBroken))
            event.setCancelled(true);
    }

}
