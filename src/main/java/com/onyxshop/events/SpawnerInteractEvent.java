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
   public void onPlayerInteract(PlayerInteractEvent var1) {
      if (var1.getAction() != Action.LEFT_CLICK_BLOCK) {
         if (var1.getClickedBlock() != null && var1.getClickedBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.isSpawnEgg(var1.getPlayer().getItemInHand())) {
               var1.setCancelled(true);
            }
         }
      }
   }

   private boolean isSpawnEgg(ItemStack var1) {
      return XMaterial.isNewVersion() ? var1.getType().name().endsWith("SPAWN_EGG") : var1.getType().name().contains("MONSTER_EGG");
   }
}
