package nova.walls.Models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import nova.walls.Controllers.GameController;
import nova.walls.Walls;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RespawnAnchor {
    private BlockState block;
    private ItemStack item;
    int team; // 1 for red, 2 for blue
    private Queue<UUID> respawnQueue;
    private int secondTimerTaskId;
    private int tickTimerTaskId;
    int respawnTime;
    boolean destroyed;

    public RespawnAnchor(int team) {
        this.item = new ItemStack(Material.RESPAWN_ANCHOR);
        ItemMeta meta = item.getItemMeta();
        this.team = team;
        if (team == 1) {
            meta.setDisplayName("Red Respawn Anchor");
        } else if (team == 2) {
            meta.setDisplayName("Blue Respawn Anchor");
        }
        meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        respawnTime = Walls.RESPAWN_TIMER_MIN;
        respawnQueue = new LinkedList<>();
        startTimer();
        destroyed = false;
    }

    public void giveRespawnAnchor(Player player) {
        player.getInventory().addItem(item);
        destroyed = false;
    }

    public void dropRespawnAnchor(Location location) {
        Walls.world.dropItemNaturally(location, item);
    }

    // return true is successful, false if otherwise
    public boolean teleportToAnchor(Player player) {
        // if block isn't placed, check if someone is holding it
        if (block == null) {
            for (UUID targetPlayerUUID : Walls.gameController.getPlayersInGame()) {
                Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                Inventory inv = targetPlayer.getInventory();
                // if it is found in someone's inventory, teleport to that person
                if (inv.contains(item)) {
                    player.teleport(targetPlayer);
                    return true;
                }
            }

            // Check all dropped items in the world
            for (Entity entity : Walls.world.getEntities()) {
                if (entity instanceof Item droppedItem) {
                    ItemStack droppedStack = droppedItem.getItemStack();
                    // Check if the dropped item matches the respawn anchor item
                    if (droppedStack.isSimilar(item)) {
                        player.teleport(droppedItem.getLocation());
                        player.sendRichMessage("<yellow>[Walls] Your respawn anchor was dropped, teleporting you to it.");
                        return true;
                    }
                }
            }

            // if it isn't found, no teleport
            return false;
        } else {
            // If block is placed, teleport to its location
            player.teleportAsync(block.getLocation().add(0.5, 1, 0.5)); // Center on the block and above it
            return true;
        }
    }

    public BlockState getBlock() {
        return block;
    }

    public void setBlock(BlockState block) {
        this.block = block;
    }

    public void spawnPlayer(Player player) {
        teleportToAnchor(player);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void startTimer() {
        secondTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Walls.instance, () -> {
            // only run if game is active
            if (Walls.gameController.getStatus() != GameController.Status.ACTIVE) return;
            // if there is no players waiting to respawn, wait at RESPAWN_TIMER_MIN
            if (respawnQueue.isEmpty() && respawnTime < Walls.RESPAWN_TIMER_MIN) respawnTime = Walls.RESPAWN_TIMER_MIN;
            // if there is a player waiting to respawn, when timer hits 0, respawn the player and reset timer to RESPAWN_TIMER_MAX
            if (respawnTime <= 0) {
                spawnPlayer(Bukkit.getPlayer(respawnQueue.poll()));
                respawnTime = Walls.RESPAWN_TIMER_MAX;
            }

            // display anchor timer
            for (UUID uuid : respawnQueue) {
                int index = respawnQueue.stream().toList().indexOf(uuid);
                Player player = Bukkit.getPlayer(uuid);
                int playerSpawnTimer = respawnTime + 60 * index;
                Component subTitle = Component.text("Respawning in " + playerSpawnTimer + " seconds", NamedTextColor.GRAY);
                Component mainTitle = Component.text("");
                Title title = Title.title(mainTitle, subTitle, 0, 30, 0);
                player.showTitle(title);
            }

            // iterate timer
            respawnTime--;
        }, 0L, 20L);

        tickTimerTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Walls.instance, () -> {
            // only run if game is active
            if (Walls.gameController.getStatus() != GameController.Status.ACTIVE) return;
            // check if respawn anchor is destroyed
            if (!destroyed) {
                if (block == null) {
                    for (UUID targetPlayerUUID : Walls.gameController.getPlayersInGame()) {
                        Player targetPlayer = Bukkit.getPlayer(targetPlayerUUID);
                        if (targetPlayer == null) continue;
                        Inventory inv = targetPlayer.getInventory();
                        // if it is found in someone's inventory, teleport to that person
                        if (inv.contains(item)) {
                            return;
                        }
                    }

                    // Check all dropped items in the world
                    for (Entity entity : Walls.world.getEntities()) {
                        if (entity instanceof Item droppedItem) {
                            ItemStack droppedStack = droppedItem.getItemStack();
                            // Check if the dropped item matches the respawn anchor item
                            if (droppedStack.isSimilar(item)) {
                                return;
                            }
                        }
                    }

                    // if it isn't found, no teleport
                    destroyed = true;
                    Bukkit.getServer().sendRichMessage("<red>[Walls] " + (team == 1 ? "Red" : "Blue") + " team's respawn anchor was destroyed!");
                    cancelSecondTimer();
                    cancelTickTimer();
                }
            }
        }, 0L, 1L);
    }

    public void cancelSecondTimer() {
        Bukkit.getScheduler().cancelTask(secondTimerTaskId);
    }

    public void cancelTickTimer() {
        Bukkit.getScheduler().cancelTask(tickTimerTaskId);
    }

    public void addToRespawnQueue(UUID uuid) {
        respawnQueue.add(uuid);
    }

    public Queue<UUID> getRespawnQueue() {
        return respawnQueue;
    }

    public int getRespawnTime() {
        return respawnTime;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
