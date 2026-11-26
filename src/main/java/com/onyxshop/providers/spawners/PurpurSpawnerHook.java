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

   public PurpurSpawnerHook(FileConfiguration var1) {
      try {
         this.itemName = ChatUtil.formatColors(var1.getString("world-settings.default.gameplay-mechanics.silk-touch.spawner-name"));
         this.itemLore = var1.getStringList("world-settings.default.gameplay-mechanics.silk-touch.spawner-lore")
            .stream()
            .map(ChatUtil::formatColors)
            .collect(Collectors.toList());
      } catch (Exception var3) {
         SendMessage.warnMessage("Failed to load spawner data from purpur.yml...");
         var3.printStackTrace();
         this.customName = false;
      }
   }

   @Override
   public String getProviderName() {
      return "PURPUR";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      String var4 = null;
      List var5 = null;
      if (var1 != null) {
         ItemMeta var6 = var2.getItemMeta();
         if (var6.hasDisplayName()) {
            var4 = var6.getDisplayName();
         }

         if (var6.hasLore()) {
            var5 = var6.getLore();
         }
      }

      if (this.customName) {
         ItemMeta var9 = var2.getItemMeta();
         var9.setDisplayName(this.itemName);
         List<String> var7 = var9.hasLore() ? var9.getLore() : new ArrayList<>();
         var7.addAll(this.itemLore);
         var7.replaceAll(var2x -> var2x.replace("<mob>", this.serializedName(var3)));
         var9.setLore((List)var7);
         var2.setItemMeta(var9);
      }

      if (var1 != null) {
         ItemMeta var10 = var2.getItemMeta();
         if (var4 != null) {
            var10.setDisplayName(var4);
         }

         if (this.itemName == null || this.itemName.isEmpty()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(var10, SpawnerManager.getDefaultName(var3));
         }

         if (var5 != null) {
            var10.setLore(var5);
         }

         var2.setItemMeta(var10);
      }

      var2 = new ItemBuilder(var2).hideEntityData().build();
      return OnyxShop.getInstance().versionHandler.setNBTString(var2, "Purpur.SpawnData::SpawnData::entity::id", var3.getKey().toString());
   }

   private String serializedName(EntityType var1) {
      return OnyxShop.getInstance().getMaterialName(var1.name());
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "display::Name", "display::Lore", "HideFlags");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      return OnyxShop.getInstance()
         .versionHandler
         .getNBTString(var1, "Purpur.SpawnData::SpawnData::entity::id")
         .replace("minecraft:", "")
         .toLowerCase(Locale.ROOT);
   }
}
