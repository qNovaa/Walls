package nova.walls.Events;

import nova.walls.Controllers.GameController;
import nova.walls.Walls;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class OnPlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (Walls.gameController.getPlayersInGame().contains(e.getPlayer().getUniqueId()) || Walls.gameController.getPlayersSpectatingUUID().contains(e.getPlayer().getUniqueId())) {
            // Player is already in game or spectating, do nothing
            return;
        }
        // Add player to game
        if (!Walls.gameController.getPlayersSpectatingUUID().contains(e.getPlayer().getUniqueId())) {
            Walls.gameController.getPlayersSpectatingUUID().add(e.getPlayer().getUniqueId());
        }

        if (Walls.gameController.getStatus() == GameController.Status.ACTIVE) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            e.getPlayer().sendRichMessage("<red>Game is already in progress, you have been added as a spectator");
        }
    }
}
