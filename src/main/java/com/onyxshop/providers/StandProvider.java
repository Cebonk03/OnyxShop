package com.onyxshop.providers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.stands.StandListener;
import com.onyxshop.files.Lang;
import com.onyxshop.files.StandStorage;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.stands.Stand;
import com.onyxshop.objects.stands.StandLoc;
import com.onyxshop.objects.stands.StandSettings;
import com.onyxshop.objects.stands.StandType;
import com.onyxshop.util.SimpleChunk;
import com.onyxshop.util.SimpleLocation;
import com.onyxshop.util.exceptions.StandLoadException;
import com.onyxshop.util.exceptions.StandUnloadException;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

public class StandProvider {
   private final OnyxShop plugin;
   private final StandStorage storage;
   private final Set<Stand> stands;
   private final Map<UUID, Stand> loaded = new HashMap<>();
   private final Set<StandLoc> spawnLocations = new HashSet<>();

   public StandProvider(OnyxShop plugin) {
      this.plugin = plugin;
      this.storage = new StandStorage(plugin);
      this.stands = this.storage.getStoredStands();
      this.plugin.getServer().getPluginManager().registerEvents(new StandListener(this.plugin, this), this.plugin);
      SendMessage.infoMessage(Lang.LOADED_SHOP_STAND_MODULE.get().replace("%count%", String.valueOf(this.stands.size())));
      this.plugin.runTaskLater(this::loadSpawnRegions, 20L);
   }

   public Stand getStand(UUID uniqueId) {
      return this.loaded.get(uniqueId);
   }

   public Stand getStand(StandLoc standLoc) {
      return this.stands.stream().filter(element -> element.getLoc().equals(standLoc)).findFirst().orElse(null);
   }

   public Stand getStand(int value) {
      return this.stands.stream().filter(element -> element.getId() == value).findFirst().orElse(null);
   }

   public Set<Stand> getStands() {
      return this.stands;
   }

   public void createStand(Block block, String type, StandType standType) throws StandLoadException {
      SimpleLocation simpleLocation = new SimpleLocation(block);
      Stand stand = new Stand(this.stands.size() + 1, simpleLocation, type, standType, new StandSettings());
      UUID uniqueId = stand.load();
      this.stands.add(stand);
      this.loaded.put(uniqueId, stand);
   }

   public void loadStands(SimpleChunk simpleChunk) {
      Set values = this.stands.stream().filter(element -> simpleChunk.contains(element.getLoc())).collect(Collectors.toSet());
      this.loadStands(values);
   }

   public boolean reloadStand(Stand stand) {
      try {
         stand.unload();
         this.loaded.remove(stand.getStand());
      } catch (StandUnloadException exception) {
         SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         return false;
      }

      this.plugin.runTaskLater(() -> {
         try {
            UUID uniqueId = stand.load();
            this.loaded.put(uniqueId, stand);
         } catch (StandLoadException exception) {
            SendMessage.errorMessage(Lang.SHOP_STAND_LOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         }
      }, 2L);
      return true;
   }

   private void loadStands(Set<Stand> values) {
      for (Stand stand : values) {
         try {
            UUID uniqueId = stand.load();
            this.loaded.put(uniqueId, stand);
         } catch (StandLoadException exception) {
            SendMessage.errorMessage(Lang.SHOP_STAND_LOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         }
      }
   }

   public void checkAndUnload(SimpleChunk simpleChunk) {
      Set values = this.loaded.values().stream().filter(element -> simpleChunk.contains(element.getLoc())).collect(Collectors.toSet());
      this.unloadStands(values);
   }

   private void unloadStands(Set<Stand> values) {
      for (Stand stand : values) {
         try {
            stand.unload();
         } catch (StandUnloadException exception) {
            SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         }
      }
   }

   private void loadSpawnRegions() {
      for (World world : Bukkit.getWorlds()) {
         this.spawnLocations.add(new StandLoc(world.getSpawnLocation()));

         for (Chunk chunk : world.getLoadedChunks()) {
            this.loadStands(new SimpleChunk(chunk));
         }
      }
   }

   public void destroy(Stand stand) {
      try {
         stand.unload();
         stand.getLoc().toBukkit().getBlock().setType(Material.AIR);
         this.stands.remove(stand);
         this.loaded.remove(stand.getStand());
         SendMessage.infoMessage(Lang.REMOVED_SHOP_STAND.get().replace("%location%", stand.getLoc().toString()));
      } catch (StandUnloadException exception) {
         SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         SendMessage.errorMessage("Forcing removal...");
         this.unloadInvalid(stand, stand.getLoc().toBukkit());
      }
   }

   private void unloadInvalid(Stand stand, Location location) {
      location.add(0.5, 0.5, 0.5);

      for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
         if (entity instanceof Item && this.matchesLoc(entity.getLocation(), location)) {
            entity.remove();
         }
      }

      location.subtract(0.0, 0.5, 0.0);

      for (Entity entity : location.getWorld().getNearbyEntities(location, 1.0, 1.0, 1.0)) {
         if (entity instanceof ArmorStand && entity.getLocation().equals(location)) {
            entity.remove();
         }
      }

      this.stands.remove(stand);
      this.loaded.remove(stand.getStand());
   }

   private boolean matchesLoc(Location location, Location location2) {
      return location.getX() == location2.getX() && location.getY() == location2.getY() && location.getZ() == location2.getZ();
   }

   public void reload() {
      this.unloadStands(new HashSet<>(this.loaded.values()));
      if (!this.loaded.isEmpty()) {
         this.loaded.clear();
      }

      this.loadSpawnRegions();
      SendMessage.infoMessage(Lang.LOADED_SHOP_STAND_MODULE.get().replace("%count%", String.valueOf(this.stands.size())));
   }

   public void stop() {
      for (Stand stand : this.loaded.values()) {
         try {
            stand.unload();
         } catch (StandUnloadException exception) {
            SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(stand.getId())).replace("%reason%", exception.getMessage()));
         }
      }

      this.storage.save(this.stands);
   }
}
