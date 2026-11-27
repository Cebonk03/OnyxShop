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

   public SpawnerBreakEvent(OnyxShop plugin) {
      this.plugin = plugin;
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
   public void onBlockBreak(BlockBreakEvent event) {
      if (!event.isCancelled()) {
         if (event.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.plugin.getSpawnerManager().getProvider().getProviderName().equalsIgnoreCase("DEFAULT")) {
               Block block = event.getBlock();
               Player player = event.getPlayer();
               CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
               if (!this.self || this.isSelf(creatureSpawner)) {
                  if (this.isAllowedTool(this.plugin.versionHandler.getItemInHand(player))) {
                     if (!this.dropEXP) {
                        event.setExpToDrop(0);
                     }

                     this.giveSpawner(player, creatureSpawner.getSpawnedType());
                  } else {
                     SendMessage.logDebugMessage(
                        "Player "
                           + player.getName()
                           + " tried to mine a spawner with a not allowed spawner-break-tool: "
                           + this.plugin.versionHandler.getItemInHand(player).toString()
                     );
                  }
               }
            }
         }
      }
   }

   private boolean isAllowedTool(ItemStack itemStack) {
      for (ItemStack itemStack2 : this.allowedTools) {
         if (itemStack.getType().equals(itemStack2.getType()) && this.hasEnchants(itemStack2, itemStack)) {
            return true;
         }
      }

      return false;
   }

   private boolean isSelf(CreatureSpawner creatureSpawner) {
      return this.plugin.version >= 114 && creatureSpawner.getPersistentDataContainer().has(new NamespacedKey(this.plugin, "SpawnerType"), PersistentDataType.STRING);
   }

   private boolean hasEnchants(ItemStack itemStack, ItemStack itemStack2) {
      for (Enchantment enchantment : itemStack.getEnchantments().keySet()) {
         if (itemStack2.getEnchantmentLevel(enchantment) < itemStack.getEnchantmentLevel(enchantment)) {
            return false;
         }
      }

      return true;
   }

   private void giveSpawner(Player player, EntityType entityType) {
      if (entityType != null) {
         ItemStack itemStack = this.plugin.versionHandler.getSpawnerToGive(entityType);
         if (!this.interact) {
            itemStack = new ItemBuilder(itemStack).hideInteractLore().build();
         }

         if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(new ItemStack[]{itemStack});
            SendMessage.chatToPlayer(
               player,
               Lang.SPAWNER_BROKE_AND_APPLIED_TO_INVENTORY
                  .get(player)
                  .replace("%spawnertype%", itemStack.getItemMeta().getDisplayName())
                  .replace("%spawner-type%", itemStack.getItemMeta().getDisplayName())
            );
         } else {
            Location location = player.getLocation();
            World world = player.getWorld();
            world.dropItem(location, itemStack);
         }
      }
   }
}
