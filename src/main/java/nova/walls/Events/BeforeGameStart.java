package nova.walls.Events;

import nova.walls.Controllers.GameController;
import nova.walls.Walls;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class BeforeGameStart implements Listener {

    private boolean isWaiting() {
        return Walls.gameController.getStatus() == GameController.Status.WAITING;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (isWaiting()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (isWaiting()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (isWaiting()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (isWaiting()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (isWaiting()) {
            e.setCancelled(true);
            Walls.gameController.giveTeamItems(e.getPlayer());
        }
    }
}
