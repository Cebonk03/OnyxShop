package com.onyxshop.events;

import com.onyxshop.util.XMaterial;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SpawnerInteractEvent implements Listener {
   public SpawnerInteractEvent() {
   }

   @EventHandler
   public void onPlayerInteract(PlayerInteractEvent event) {
      if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
         if (event.getClickedBlock() != null && event.getClickedBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.isSpawnEgg(event.getPlayer().getItemInHand())) {
               event.setCancelled(true);
            }
         }
      }
   }

   private boolean isSpawnEgg(ItemStack itemStack) {
      return XMaterial.isNewVersion() ? itemStack.getType().name().endsWith("SPAWN_EGG") : itemStack.getType().name().contains("MONSTER_EGG");
   }
}
