package com.onyxshop.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.util.XMaterial;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerPlaceEvent implements Listener {
   private final boolean self;
   private final OnyxShop plugin;

   public SpawnerPlaceEvent(OnyxShop plugin) {
      this.plugin = plugin;
      this.self = ConfigManager.getConfig().getBoolean("only-mine-plugin-spawners");
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      if (!event.isCancelled()) {
         if (event.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.plugin.getSpawnerManager().getProvider().getProviderName().equalsIgnoreCase("DEFAULT")) {
               Block block = event.getBlock();
               String spawnerType = this.plugin.versionHandler.getSpawnerType(event.getItemInHand());
               if (spawnerType != null) {
                  EntityType entityType = EntityType.valueOf(spawnerType.toUpperCase());
                  CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                  if (this.self && this.plugin.version >= 114) {
                     creatureSpawner.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "SpawnerType"), PersistentDataType.STRING, entityType.name());
                  }

                  creatureSpawner.setSpawnedType(entityType);
                  creatureSpawner.update();
               }
            }
         }
      }
   }
}
