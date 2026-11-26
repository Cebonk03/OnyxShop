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

   public StandProvider(OnyxShop var1) {
      this.plugin = var1;
      this.storage = new StandStorage(var1);
      this.stands = this.storage.getStoredStands();
      this.plugin.getServer().getPluginManager().registerEvents(new StandListener(this.plugin, this), this.plugin);
      SendMessage.infoMessage(Lang.LOADED_SHOP_STAND_MODULE.get().replace("%count%", String.valueOf(this.stands.size())));
      this.plugin.runTaskLater(this::loadSpawnRegions, 20L);
   }

   public Stand getStand(UUID var1) {
      return this.loaded.get(var1);
   }

   public Stand getStand(StandLoc var1) {
      return this.stands.stream().filter(var1x -> var1x.getLoc().equals(var1)).findFirst().orElse(null);
   }

   public Stand getStand(int var1) {
      return this.stands.stream().filter(var1x -> var1x.getId() == var1).findFirst().orElse(null);
   }

   public Set<Stand> getStands() {
      return this.stands;
   }

   public void createStand(Block var1, String var2, StandType var3) throws StandLoadException {
      SimpleLocation var4 = new SimpleLocation(var1);
      Stand var5 = new Stand(this.stands.size() + 1, var4, var2, var3, new StandSettings());
      UUID var6 = var5.load();
      this.stands.add(var5);
      this.loaded.put(var6, var5);
   }

   public void loadStands(SimpleChunk var1) {
      Set var2 = this.stands.stream().filter(var1x -> var1.contains(var1x.getLoc())).collect(Collectors.toSet());
      this.loadStands(var2);
   }

   public boolean reloadStand(Stand var1) {
      try {
         var1.unload();
         this.loaded.remove(var1.getStand());
      } catch (StandUnloadException var3) {
         SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(var1.getId())).replace("%reason%", var3.getMessage()));
         return false;
      }

      this.plugin.runTaskLater(() -> {
         try {
            UUID var2 = var1.load();
            this.loaded.put(var2, var1);
         } catch (StandLoadException var3x) {
            SendMessage.errorMessage(Lang.SHOP_STAND_LOAD_FAILED.get().replace("%id%", String.valueOf(var1.getId())).replace("%reason%", var3x.getMessage()));
         }
      }, 2L);
      return true;
   }

   private void loadStands(Set<Stand> var1) {
      for (Stand var3 : var1) {
         try {
            UUID var4 = var3.load();
            this.loaded.put(var4, var3);
         } catch (StandLoadException var5) {
            SendMessage.errorMessage(Lang.SHOP_STAND_LOAD_FAILED.get().replace("%id%", String.valueOf(var3.getId())).replace("%reason%", var5.getMessage()));
         }
      }
   }

   public void checkAndUnload(SimpleChunk var1) {
      Set var2 = this.loaded.values().stream().filter(var1x -> var1.contains(var1x.getLoc())).collect(Collectors.toSet());
      this.unloadStands(var2);
   }

   private void unloadStands(Set<Stand> var1) {
      for (Stand var3 : var1) {
         try {
            var3.unload();
         } catch (StandUnloadException var5) {
            SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(var3.getId())).replace("%reason%", var5.getMessage()));
         }
      }
   }

   private void loadSpawnRegions() {
      for (World var2 : Bukkit.getWorlds()) {
         this.spawnLocations.add(new StandLoc(var2.getSpawnLocation()));

         for (Chunk var6 : var2.getLoadedChunks()) {
            this.loadStands(new SimpleChunk(var6));
         }
      }
   }

   public void destroy(Stand var1) {
      try {
         var1.unload();
         var1.getLoc().toBukkit().getBlock().setType(Material.AIR);
         this.stands.remove(var1);
         this.loaded.remove(var1.getStand());
         SendMessage.infoMessage(Lang.REMOVED_SHOP_STAND.get().replace("%location%", var1.getLoc().toString()));
      } catch (StandUnloadException var3) {
         SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(var1.getId())).replace("%reason%", var3.getMessage()));
         SendMessage.errorMessage("Forcing removal...");
         this.unloadInvalid(var1, var1.getLoc().toBukkit());
      }
   }

   private void unloadInvalid(Stand var1, Location var2) {
      var2.add(0.5, 0.5, 0.5);

      for (Entity var5 : var2.getWorld().getNearbyEntities(var2, 1.0, 1.0, 1.0)) {
         if (var5 instanceof Item && this.matchesLoc(var5.getLocation(), var2)) {
            var5.remove();
         }
      }

      var2.subtract(0.0, 0.5, 0.0);

      for (Entity var8 : var2.getWorld().getNearbyEntities(var2, 1.0, 1.0, 1.0)) {
         if (var8 instanceof ArmorStand && var8.getLocation().equals(var2)) {
            var8.remove();
         }
      }

      this.stands.remove(var1);
      this.loaded.remove(var1.getStand());
   }

   private boolean matchesLoc(Location var1, Location var2) {
      return var1.getX() == var2.getX() && var1.getY() == var2.getY() && var1.getZ() == var2.getZ();
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
      for (Stand var2 : this.loaded.values()) {
         try {
            var2.unload();
         } catch (StandUnloadException var4) {
            SendMessage.errorMessage(Lang.SHOP_STAND_UNLOAD_FAILED.get().replace("%id%", String.valueOf(var2.getId())).replace("%reason%", var4.getMessage()));
         }
      }

      this.storage.save(this.stands);
   }
}
