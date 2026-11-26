package com.onyxshop.providers.spawners;

import java.util.List;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.NBTMaps;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class EpicSpawnersHook implements SpawnerProvider {
   private final OnyxShop plugin;

   public EpicSpawnersHook(OnyxShop var1) {
      this.plugin = var1;
   }

   @Override
   public String getProviderName() {
      return "EPICSPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      var2 = this.plugin.versionHandler.setNBTInt(var2, "size", 1);
      var2 = this.plugin.versionHandler.setNBTString(var2, "data", var3.name());
      var2 = this.plugin.versionHandler.setNBTString(var2, "tier", "Tier_1");
      ItemMeta var4 = var2.getItemMeta();
      if (!var4.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
      }

      var2.setItemMeta(var4);
      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "size", "tier", "display::Name");
      var4.addAll(var3);
      return this.plugin.isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      Integer var4 = this.plugin.versionHandler.getNBTInt(var1, NBTMaps.CUSTOM_DATA.getTag() + "size");
      var4 = var4 == null ? 1 : var4;
      return var4.intValue() * var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      return this.plugin.versionHandler.getNBTString(var1, "data");
   }
}
