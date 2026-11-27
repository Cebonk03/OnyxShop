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

   public UpgradeableSpawnersHook(OnyxShop plugin) {
      this.plugin = plugin;
      File file = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/UpgradeableSpawners/entities.yml");
      if (file.exists()) {
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
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      if (!this.isEnabled(entityType)) {
         throw new ItemLoadException("Entity '" + entityType.name().toLowerCase(Locale.ENGLISH) + "' is not enabled inside the entities.yml of UpgradeableSpawners.");
      } else {
         if (shopItem == null) {
            itemStack = this.plugin.versionHandler.setNBTInt(itemStack, "PublicBukkitValues::upgradeablespawners:uspawners", 1);
            itemStack = this.plugin
               .versionHandler
               .setNBTString(
                  itemStack,
                  "PublicBukkitValues::upgradeablespawners:l",
                  "\"NEARBY_ENTITIES:1\",\"PLAYER_RANGE:1\",\"PERIOD_AMOUNT:1\",\"SPAWN_AMOUNT:1\",\"SPAWN_INTERVAL:1\""
               );
            itemStack = this.plugin.versionHandler.setNBTInt(itemStack, "PublicBukkitValues::upgradeablespawners:entities", 0);
            itemStack = this.plugin.versionHandler.setNBTInt(itemStack, "PublicBukkitValues::upgradeablespawners:period", 0);
            itemStack = this.plugin.versionHandler.setNBTString(itemStack, "PublicBukkitValues::upgradeablespawners:entity", entityType.name());
            itemStack = this.plugin.versionHandler.setNBTString(itemStack, "PublicBukkitValues::upgradeablespawners:bought", "");
         } else {
            shopItem.addTaggable(TaggableItemType.UPGRADEABLE_SPAWNERS);
         }

         itemStack = this.plugin.versionHandler.setNBTInt(itemStack, "HideFlags", -1);
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (!itemMeta.hasDisplayName()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
         }

         itemStack.setItemMeta(itemMeta);
         return itemStack;
      }
   }

   private boolean isEnabled(EntityType entityType) {
      return this.entities == null ? true : this.entities.getBoolean("types." + entityType.name().toLowerCase(Locale.ENGLISH) + ".enabled", true);
   }

   public static ItemStack tag(ItemStack itemStack, Player player) {
      return OnyxShop.getInstance().versionHandler.setNBTString(itemStack, "PublicBukkitValues::upgradeablespawners:o", player.getUniqueId().toString());
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      try {
         List values = NBTMaps.getTags(
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
         values.addAll(shopIds);
         return new UpgradeableSpawnersHook.SpawnerData(itemStack).equals(new UpgradeableSpawnersHook.SpawnerData(itemStack2)) && this.plugin.isSimilar(itemStack, itemStack2, values);
      } catch (IllegalArgumentException exception) {
         exception.printStackTrace();
         return false;
      }
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      String nbtString = this.plugin.versionHandler.getNBTString(itemStack, "PublicBukkitValues::upgradeablespawners:entity");

      try {
         return EntityType.valueOf(nbtString.toUpperCase()).name();
      } catch (IllegalArgumentException exception) {
         SendMessage.warnMessage("Could not load entity type '" + nbtString + "'");
         return null;
      }
   }

   private class SpawnerData {
      private int NEARBY_ENTITIES = 0;
      private int PLAYER_RANGE = 0;
      private int PERIOD_AMOUNT = 0;
      private int SPAWN_AMOUNT = 0;
      private int SPAWN_INTERVAL = 0;

      public SpawnerData(ItemStack itemStack) {
         String nbtString = UpgradeableSpawnersHook.this.plugin.versionHandler.getNBTString(itemStack, "PublicBukkitValues::upgradeablespawners:l");
         if (nbtString == null) {
            throw new IllegalArgumentException("Data cannot be null");
         } else {
            String[] values = nbtString.replace("\"", "").split(",");

            for (String text : values) {
               switch (UpgradeableSpawnersHook.SpawnerSettings.valueOf(text.split(":")[0])) {
                  case NEARBY_ENTITIES:
                     this.NEARBY_ENTITIES = Integer.parseInt(text.split(":")[1]);
                     break;
                  case PLAYER_RANGE:
                     this.PLAYER_RANGE = Integer.parseInt(text.split(":")[1]);
                     break;
                  case PERIOD_AMOUNT:
                     this.PERIOD_AMOUNT = Integer.parseInt(text.split(":")[1]);
                     break;
                  case SPAWN_AMOUNT:
                     this.SPAWN_AMOUNT = Integer.parseInt(text.split(":")[1]);
                     break;
                  case SPAWN_INTERVAL:
                     this.SPAWN_INTERVAL = Integer.parseInt(text.split(":")[1]);
               }
            }
         }
      }

      @Override
      public boolean equals(Object value) {
         if (this == value) {
            return true;
         } else if (value != null && this.getClass() == value.getClass()) {
            UpgradeableSpawnersHook.SpawnerData spawnerData = (UpgradeableSpawnersHook.SpawnerData)value;
            return this.NEARBY_ENTITIES == spawnerData.NEARBY_ENTITIES
               && this.PLAYER_RANGE == spawnerData.PLAYER_RANGE
               && this.PERIOD_AMOUNT == spawnerData.PERIOD_AMOUNT
               && this.SPAWN_AMOUNT == spawnerData.SPAWN_AMOUNT
               && this.SPAWN_INTERVAL == spawnerData.SPAWN_INTERVAL;
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
