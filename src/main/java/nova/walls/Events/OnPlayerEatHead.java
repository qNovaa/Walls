package nova.walls.Events;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class OnPlayerEatHead implements Listener {
    @EventHandler
    public void onPlayerEatHead(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if (e.getAction().isRightClick() && e.getItem() != null && e.getItem().getType() == Material.PLAYER_HEAD) {
            ItemStack item = e.getItem();

            player.playSound(player, Sound.ENTITY_GENERIC_EAT, 1, 1);

            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 120, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 300, 0));

            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().remove(item);
            }
        }
    }
}
