package com.onyxshop.providers.spawners;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.api.SmartSpawnerAPI;
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

public class SmartSpawnerHook implements SpawnerProvider {
   private final SmartSpawnerAPI api = SmartSpawner.getInstance().getAPI();

   public SmartSpawnerHook() {
   }

   @Override
   public String getProviderName() {
      return "SMART_SPAWNER";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      String var4 = null;
      List var5 = null;
      if (var2.hasItemMeta()) {
         ItemMeta var6 = var2.getItemMeta();
         if (var6.hasDisplayName()) {
            var4 = var6.getDisplayName();
         }

         if (var6.hasLore()) {
            var5 = var6.getLore();
         }
      }

      var2 = this.api.createSpawnerItem(var3);
      if (var2.hasItemMeta()) {
         ItemMeta var8 = var2.getItemMeta();
         if (var4 != null) {
            var8.setDisplayName(var4);
         }

         if (!var8.hasDisplayName()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(var8, SpawnerManager.getDefaultName(var3));
         }

         if (var5 != null) {
            var8.setLore(var5);
         }

         var2.setItemMeta(var8);
      }

      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "display::Lore", "display::Name", "HideFlags");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      return this.api.getSpawnerEntityType(var1).name();
   }
}
