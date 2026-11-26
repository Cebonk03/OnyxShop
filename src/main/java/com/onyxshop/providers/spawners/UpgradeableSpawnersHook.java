package com.onyxshop.providers.spawners;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.TaggableItemType;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class UpgradeableSpawnersHook implements SpawnerProvider {
   private static final String AMOUNT = "PublicBukkitValues::upgradeablespawners:uspawners";
   private static final String OWNER = "PublicBukkitValues::upgradeablespawners:o";
   private static final String DATA = "PublicBukkitValues::upgradeablespawners:l";
   private static final String PERIOD = "PublicBukkitValues::upgradeablespawners:period";
   private static final String BOUGHT = "PublicBukkitValues::upgradeablespawners:bought";
   private static final String ENTITIES = "PublicBukkitValues::upgradeablespawners:entities";
   private static final String ENTITY = "PublicBukkitValues::upgradeablespawners:entity";
   private final OnyxShop plugin;
   private FileConfiguration entities;

   public UpgradeableSpawnersHook(OnyxShop var1) {
      this.plugin = var1;
      File var2 = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/UpgradeableSpawners/entities.yml");
      if (var2.exists()) {
         this.entities = OnyxShop.getInstance()
            .loadConfiguration(
               new File(OnyxShop.getInstance().getDataFolder().getParent() + "/UpgradeableSpawners/entities.yml"), "Upgradeable Spawners config"
            );
      } else {
         SendMessage.warnMessage("Could not find the entities.yml of UpgradeableSpawners, using default spawner settings...");
      }
   }

   @Override
   public String getProviderName() {
      return "UPGRADEABLE_SPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      if (!this.isEnabled(var3)) {
         throw new ItemLoadException("Entity '" + var3.name().toLowerCase(Locale.ENGLISH) + "' is not enabled inside the entities.yml of UpgradeableSpawners.");
      } else {
         if (var1 == null) {
            var2 = this.plugin.versionHandler.setNBTInt(var2, "PublicBukkitValues::upgradeablespawners:uspawners", 1);
            var2 = this.plugin
               .versionHandler
               .setNBTString(
                  var2,
                  "PublicBukkitValues::upgradeablespawners:l",
                  "\"NEARBY_ENTITIES:1\",\"PLAYER_RANGE:1\",\"PERIOD_AMOUNT:1\",\"SPAWN_AMOUNT:1\",\"SPAWN_INTERVAL:1\""
               );
            var2 = this.plugin.versionHandler.setNBTInt(var2, "PublicBukkitValues::upgradeablespawners:entities", 0);
            var2 = this.plugin.versionHandler.setNBTInt(var2, "PublicBukkitValues::upgradeablespawners:period", 0);
            var2 = this.plugin.versionHandler.setNBTString(var2, "PublicBukkitValues::upgradeablespawners:entity", var3.name());
            var2 = this.plugin.versionHandler.setNBTString(var2, "PublicBukkitValues::upgradeablespawners:bought", "");
         } else {
            var1.addTaggable(TaggableItemType.UPGRADEABLE_SPAWNERS);
         }

         var2 = this.plugin.versionHandler.setNBTInt(var2, "HideFlags", -1);
         ItemMeta var4 = var2.getItemMeta();
         if (!var4.hasDisplayName()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
         }

         var2.setItemMeta(var4);
         return var2;
      }
   }

   private boolean isEnabled(EntityType var1) {
      return this.entities == null ? true : this.entities.getBoolean("types." + var1.name().toLowerCase(Locale.ENGLISH) + ".enabled", true);
   }

   public static ItemStack tag(ItemStack var0, Player var1) {
      return OnyxShop.getInstance().versionHandler.setNBTString(var0, "PublicBukkitValues::upgradeablespawners:o", var1.getUniqueId().toString());
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      try {
         List var4 = NBTMaps.getTags(
            false,
            "PublicBukkitValues::upgradeablespawners:l",
            "PublicBukkitValues::upgradeablespawners:bought",
            "PublicBukkitValues::upgradeablespawners:period",
            "PublicBukkitValues::upgradeablespawners:entities",
            "PublicBukkitValues::upgradeablespawners:o",
            "display::Name",
            "display::Lore",
            "HideFlags"
         );
         var4.addAll(var3);
         return new UpgradeableSpawnersHook.SpawnerData(var1).equals(new UpgradeableSpawnersHook.SpawnerData(var2)) && this.plugin.isSimilar(var1, var2, var4);
      } catch (IllegalArgumentException var5) {
         var5.printStackTrace();
         return false;
      }
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      String var2 = this.plugin.versionHandler.getNBTString(var1, "PublicBukkitValues::upgradeablespawners:entity");

      try {
         return EntityType.valueOf(var2.toUpperCase()).name();
      } catch (IllegalArgumentException var4) {
         SendMessage.warnMessage("Could not load entity type '" + var2 + "'");
         return null;
      }
   }

   private class SpawnerData {
      private int NEARBY_ENTITIES = 0;
      private int PLAYER_RANGE = 0;
      private int PERIOD_AMOUNT = 0;
      private int SPAWN_AMOUNT = 0;
      private int SPAWN_INTERVAL = 0;

      public SpawnerData(ItemStack nullx) {
         String var3 = UpgradeableSpawnersHook.this.plugin.versionHandler.getNBTString(nullx, "PublicBukkitValues::upgradeablespawners:l");
         if (var3 == null) {
            throw new IllegalArgumentException("Data cannot be null");
         } else {
            String[] var4 = var3.replace("\"", "").split(",");

            for (String var8 : var4) {
               switch (UpgradeableSpawnersHook.SpawnerSettings.valueOf(var8.split(":")[0])) {
                  case NEARBY_ENTITIES:
                     this.NEARBY_ENTITIES = Integer.parseInt(var8.split(":")[1]);
                     break;
                  case PLAYER_RANGE:
                     this.PLAYER_RANGE = Integer.parseInt(var8.split(":")[1]);
                     break;
                  case PERIOD_AMOUNT:
                     this.PERIOD_AMOUNT = Integer.parseInt(var8.split(":")[1]);
                     break;
                  case SPAWN_AMOUNT:
                     this.SPAWN_AMOUNT = Integer.parseInt(var8.split(":")[1]);
                     break;
                  case SPAWN_INTERVAL:
                     this.SPAWN_INTERVAL = Integer.parseInt(var8.split(":")[1]);
               }
            }
         }
      }

      @Override
      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 != null && this.getClass() == var1.getClass()) {
            UpgradeableSpawnersHook.SpawnerData var2 = (UpgradeableSpawnersHook.SpawnerData)var1;
            return this.NEARBY_ENTITIES == var2.NEARBY_ENTITIES
               && this.PLAYER_RANGE == var2.PLAYER_RANGE
               && this.PERIOD_AMOUNT == var2.PERIOD_AMOUNT
               && this.SPAWN_AMOUNT == var2.SPAWN_AMOUNT
               && this.SPAWN_INTERVAL == var2.SPAWN_INTERVAL;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.NEARBY_ENTITIES, this.PLAYER_RANGE, this.PERIOD_AMOUNT, this.SPAWN_AMOUNT, this.SPAWN_INTERVAL);
      }
   }

   private static enum SpawnerSettings {
      NEARBY_ENTITIES,
      PLAYER_RANGE,
      PERIOD_AMOUNT,
      SPAWN_AMOUNT,
      SPAWN_INTERVAL;

      private SpawnerSettings() {
      }
   }
}
