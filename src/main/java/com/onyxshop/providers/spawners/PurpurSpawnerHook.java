package com.onyxshop.providers.spawners;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.NBTMaps;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class PurpurSpawnerHook implements SpawnerProvider {
   private String itemName = ChatColor.WHITE + "Monster Spawner";
   private List<String> itemLore = new ArrayList<>();
   private boolean customName = true;

   public PurpurSpawnerHook(FileConfiguration fileConfiguration) {
      try {
         this.itemName = ChatUtil.formatColors(fileConfiguration.getString("world-settings.default.gameplay-mechanics.silk-touch.spawner-name"));
         this.itemLore = fileConfiguration.getStringList("world-settings.default.gameplay-mechanics.silk-touch.spawner-lore")
            .stream()
            .map(ChatUtil::formatColors)
            .collect(Collectors.toList());
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to load spawner data from purpur.yml...");
         exception.printStackTrace();
         this.customName = false;
      }
   }

   @Override
   public String getProviderName() {
      return "PURPUR";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      String type = null;
      List values = null;
      if (shopItem != null) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            type = itemMeta.getDisplayName();
         }

         if (itemMeta.hasLore()) {
            values = itemMeta.getLore();
         }
      }

      if (this.customName) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         itemMeta.setDisplayName(this.itemName);
         List<String> lores = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
         lores.addAll(this.itemLore);
         lores.replaceAll(entry -> entry.replace("<mob>", this.serializedName(entityType)));
         itemMeta.setLore((List)lores);
         itemStack.setItemMeta(itemMeta);
      }

      if (shopItem != null) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (type != null) {
            itemMeta.setDisplayName(type);
         }

         if (this.itemName == null || this.itemName.isEmpty()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
         }

         if (values != null) {
            itemMeta.setLore(values);
         }

         itemStack.setItemMeta(itemMeta);
      }

      itemStack = new ItemBuilder(itemStack).hideEntityData().build();
      return OnyxShop.getInstance().versionHandler.setNBTString(itemStack, "Purpur.SpawnData::SpawnData::entity::id", entityType.getKey().toString());
   }

   private String serializedName(EntityType entityType) {
      return OnyxShop.getInstance().getMaterialName(entityType.name());
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "display::Name", "display::Lore", "HideFlags");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      return OnyxShop.getInstance()
         .versionHandler
         .getNBTString(itemStack, "Purpur.SpawnData::SpawnData::entity::id")
         .replace("minecraft:", "")
         .toLowerCase(Locale.ROOT);
   }
}
