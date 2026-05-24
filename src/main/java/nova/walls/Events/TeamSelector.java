package nova.walls.Events;

import nova.walls.Walls;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class TeamSelector implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta()) {
                String displayName = item.getItemMeta().getDisplayName();
                if ("Join Red Team".equals(displayName)) {
                    Walls.gameController.addToTeam1(player.getUniqueId());
                    player.sendRichMessage("<green>[Walls] Joined <red>Red <green>Team!");
                    Walls.gameController.giveTeamItems(player);
                } else if ("Join Blue Team".equals(displayName)) {
                    Walls.gameController.addToTeam2(player.getUniqueId());
                    player.sendRichMessage("<green>[Walls] Joined <blue>Blue <green>Team!");
                    Walls.gameController.giveTeamItems(player);
                } else if ("Spectate".equals(displayName)) {
                    Walls.gameController.addToSpectators(player.getUniqueId());
                    player.sendRichMessage("<green>[Walls] Joined <white>Spectators<green>!");
                    Walls.gameController.giveTeamItems(player);
                }
            }
        }
    }
}
