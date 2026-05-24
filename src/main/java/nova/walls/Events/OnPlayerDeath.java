package nova.walls.Events;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import nova.walls.Controllers.GameController;
import nova.walls.Models.RespawnAnchor;
import nova.walls.Walls;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class OnPlayerDeath implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        GameController gameController = Walls.gameController;
        if (gameController.getStatus() != GameController.Status.ACTIVE)
            return;

        e.setCancelled(true);
        Player player = e.getPlayer();

        // go blind and switch to spectator
        PotionEffect blindness = new PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 40, 1, true, false, false);
        player.addPotionEffect(blindness);
        player.setGameMode(GameMode.SPECTATOR);

        // drop head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skullMeta = (SkullMeta)head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        head.setItemMeta(skullMeta);
        Walls.world.dropItemNaturally(player.getLocation(), head);

        // add one to death count
        HashMap<UUID, Integer> deathCounters = gameController.getDeathCounters();
        if (deathCounters.containsKey(player.getUniqueId())) {
            deathCounters.put(player.getUniqueId(), deathCounters.get(player.getUniqueId()) + 1);
        } else {
            deathCounters.put(player.getUniqueId(), 1);
        }

        // drop percentage of inventory based on death count, or everything if anchor is destroyed
        boolean isPlayerAnchorDestroyed = gameController.getRespawnAnchorOfTeam(gameController.getTeamOfPlayer(player)).isDestroyed();
        double dropChance =
                isPlayerAnchorDestroyed ?
                        1 :
                        deathCountToDropChange(deathCounters.get(player.getUniqueId()));

        // get list of different materials in inventory
        Inventory inv = player.getInventory();
        ArrayList<Material> differentItems = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) != null) {
                if (!differentItems.contains(inv.getItem(i).getType())) {
                    differentItems.add(inv.getItem(i).getType());
                }
            }
        }

        // determine what items to drop
        int numberOfMaterialsToDrop = (int)Math.ceil(differentItems.size() * dropChance);

        for (int i = 0; i < numberOfMaterialsToDrop; i++) {
            int indexToDrop = (int)(Math.random() * differentItems.size());
            Material materialToDrop = differentItems.get(indexToDrop);
            differentItems.remove(indexToDrop);

            for (int j = 0; j < inv.getSize(); j++) {
                if (inv.getItem(j) != null && inv.getItem(j).getType() == materialToDrop) {
                    Walls.world.dropItemNaturally(player.getLocation(), inv.getItem(j));
                    inv.setItem(j, null);
                }
            }
        }

        if (dropChance != 1)
            player.sendRichMessage("<red>You dropped " + dropChance * 100 + "% of your items. Next time, you will drop " + deathCountToDropChange(deathCounters.get(player.getUniqueId()) + 1) * 100 + "% of your items.");

        // add to respawn queue
        RespawnAnchor anchor = gameController.getRespawnAnchorOfTeam(gameController.getTeamOfPlayer(player));
        anchor.addToRespawnQueue(player.getUniqueId());

        // if player anchor is destroyed, display message
        if (isPlayerAnchorDestroyed) {
            Component mainTitle = Component.text("");
            Component subTitle = Component.text("Cannot respawn. Anchor is destroyed", NamedTextColor.DARK_RED);
            player.showTitle(Title.title(mainTitle, subTitle, 0, 30, 20));
        }

    }

    private double deathCountToDropChange(int deathCount) {
        double dropChance;
        switch (deathCount) {
            case 1 -> dropChance = 0.1;
            case 2 -> dropChance = 0.3;
            case 3 -> dropChance = 0.5;
            case 4 -> dropChance = 0.75;
            default -> dropChance = 1;
        }
        return dropChance;
    }
}
