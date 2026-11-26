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

   public SpawnerPlaceEvent(OnyxShop var1) {
      this.plugin = var1;
      this.self = ConfigManager.getConfig().getBoolean("only-mine-plugin-spawners");
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onBlockPlace(BlockPlaceEvent var1) {
      if (!var1.isCancelled()) {
         if (var1.getBlock().getType() == XMaterial.SPAWNER.parseMaterial()) {
            if (this.plugin.getSpawnerManager().getProvider().getProviderName().equalsIgnoreCase("DEFAULT")) {
               Block var2 = var1.getBlock();
               String var3 = this.plugin.versionHandler.getSpawnerType(var1.getItemInHand());
               if (var3 != null) {
                  EntityType var4 = EntityType.valueOf(var3.toUpperCase());
                  CreatureSpawner var5 = (CreatureSpawner)var2.getState();
                  if (this.self && this.plugin.version >= 114) {
                     var5.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "SpawnerType"), PersistentDataType.STRING, var4.name());
                  }

                  var5.setSpawnedType(var4);
                  var5.update();
               }
            }
         }
      }
   }
}
