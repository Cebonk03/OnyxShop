package com.onyxshop.events;

import java.util.ArrayList;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerBreakEvent implements Listener {
   private final boolean self;
   private final OnyxShop plugin;
   private final List<ItemStack> allowedTools = new ArrayList<>();
   private final boolean dropEXP;
   private final boolean interact;

   public SpawnerBreakEvent(OnyxShop var1) {
      this.plugin = var1;
      this.dropEXP = ConfigManager.getConfig().getBoolean("player-placed-spawners-drop-exp");
      this.self = ConfigManager.getConfig().getBoolean("only-mine-plugin-spawners");
      this.interact = ConfigManager.getConfig().getBoolean("allow-spawner-type-change");
   }

   public void initDelayed() {
      this.allowedTools.addAll(this.plugin.createItem.getAllowedTools());
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockBreak(BlockBreakEvent var1) {
      if (!var1.isCancelled()) {
         if (var1.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.plugin.getSpawnerManager().getProvider().getProviderName().equalsIgnoreCase("DEFAULT")) {
               Block var2 = var1.getBlock();
               Player var3 = var1.getPlayer();
               CreatureSpawner var4 = (CreatureSpawner)var2.getState();
               if (!this.self || this.isSelf(var4)) {
                  if (this.isAllowedTool(this.plugin.versionHandler.getItemInHand(var3))) {
                     if (!this.dropEXP) {
                        var1.setExpToDrop(0);
                     }

                     this.giveSpawner(var3, var4.getSpawnedType());
                  } else {
                     SendMessage.logDebugMessage(
                        "Player "
                           + var3.getName()
                           + " tried to mine a spawner with a not allowed spawner-break-tool: "
                           + this.plugin.versionHandler.getItemInHand(var3).toString()
                     );
                  }
               }
            }
         }
      }
   }

   private boolean isAllowedTool(ItemStack var1) {
      for (ItemStack var3 : this.allowedTools) {
         if (var1.getType().equals(var3.getType()) && this.hasEnchants(var3, var1)) {
            return true;
         }
      }

      return false;
   }

   private boolean isSelf(CreatureSpawner var1) {
      return this.plugin.version >= 114 && var1.getPersistentDataContainer().has(new NamespacedKey(this.plugin, "SpawnerType"), PersistentDataType.STRING);
   }

   private boolean hasEnchants(ItemStack var1, ItemStack var2) {
      for (Enchantment var4 : var1.getEnchantments().keySet()) {
         if (var2.getEnchantmentLevel(var4) < var1.getEnchantmentLevel(var4)) {
            return false;
         }
      }

      return true;
   }

   private void giveSpawner(Player var1, EntityType var2) {
      if (var2 != null) {
         ItemStack var3 = this.plugin.versionHandler.getSpawnerToGive(var2);
         if (!this.interact) {
            var3 = new ItemBuilder(var3).hideInteractLore().build();
         }

         if (var1.getInventory().firstEmpty() != -1) {
            var1.getInventory().addItem(new ItemStack[]{var3});
            SendMessage.chatToPlayer(
               var1,
               Lang.SPAWNER_BROKE_AND_APPLIED_TO_INVENTORY
                  .get(var1)
                  .replace("%spawnertype%", var3.getItemMeta().getDisplayName())
                  .replace("%spawner-type%", var3.getItemMeta().getDisplayName())
            );
         } else {
            Location var4 = var1.getLocation();
            World var5 = var1.getWorld();
            var5.dropItem(var4, var3);
         }
      }
   }
}
