package nova.walls.Events;

import nova.walls.Controllers.GameController;
import nova.walls.Models.RespawnAnchor;
import nova.walls.Walls;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class RespawnAnchorListener implements Listener {
    @EventHandler
    public void onRespawnAnchorPlace(BlockPlaceEvent e) {
        if (Walls.gameController.getStatus() != GameController.Status.ACTIVE) return;
        BlockState block = e.getBlock().getState();
        ItemStack item = e.getItemInHand();
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals("Red Respawn Anchor")) {
            Walls.gameController.getTeam1Anchor().setBlock(block);
        } else if (displayName.equals("Blue Respawn Anchor")) {
            Walls.gameController.getTeam2Anchor().setBlock(block);
        }
    }

    @EventHandler
    public void onRespawnAnchorBreak(BlockBreakEvent e) {
        RespawnAnchor team1Anchor = Walls.gameController.getTeam1Anchor();
        RespawnAnchor team2Anchor = Walls.gameController.getTeam2Anchor();
        BlockState block = e.getBlock().getState();
        if (block.equals(team1Anchor.getBlock())) {
            e.setDropItems(false);
            team1Anchor.dropRespawnAnchor(block.getLocation());
            team1Anchor.setBlock(null);
        } else if (block.equals(team2Anchor.getBlock())) {
            e.setDropItems(false);
            team2Anchor.dropRespawnAnchor(block.getLocation());
            team2Anchor.setBlock(null);
        }
    }

    @EventHandler
    public void onRespawnAnchorExplode(BlockExplodeEvent e) {
        RespawnAnchor team1Anchor = Walls.gameController.getTeam1Anchor();
        RespawnAnchor team2Anchor = Walls.gameController.getTeam2Anchor();
        BlockState block = e.getExplodedBlockState();
        if (!team1Anchor.isDestroyed() && block.getLocation().equals(team1Anchor.getBlock().getLocation())) {
            System.out.println("Team 1 anchor destroyed by explosion");
            team1Anchor.setBlock(null);
        } else if (!team2Anchor.isDestroyed() && block.getLocation().equals(team2Anchor.getBlock().getLocation())) {
            System.out.println("Team 2 anchor destroyed by explosion");
            team2Anchor.setBlock(null);
        }
    }
}
