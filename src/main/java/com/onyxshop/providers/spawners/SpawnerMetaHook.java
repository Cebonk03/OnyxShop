package com.onyxshop.providers.spawners;

import java.util.List;
import mc.rellox.spawnermeta.api.spawner.IVirtual;
import mc.rellox.spawnermeta.spawner.type.SpawnerType;
import mc.rellox.spawnermeta.utility.DataManager;
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

public class SpawnerMetaHook implements SpawnerProvider {
   public SpawnerMetaHook() {
   }

   @Override
   public String getProviderName() {
      return "SPAWNERMETA";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      SpawnerType var4 = SpawnerType.of(var3);
      String var5 = null;
      List var6 = null;
      if (var2.hasItemMeta()) {
         ItemMeta var7 = var2.getItemMeta();
         if (var7.hasDisplayName()) {
            var5 = var7.getDisplayName();
         }

         if (var7.hasLore()) {
            var6 = var7.getLore();
         }
      }

      var2 = DataManager.getSpawner(var4, 1);
      if (var2.hasItemMeta()) {
         ItemMeta var9 = var2.getItemMeta();
         if (var5 != null) {
            var9.setDisplayName(var5);
         }

         if (!var9.hasDisplayName()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(var9, SpawnerManager.getDefaultName(var3));
         }

         if (var6 != null) {
            var9.setLore(var6);
         }

         var2.setItemMeta(var9);
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
      IVirtual var2 = IVirtual.of(var1);
      return var2 == null ? null : var2.getType().entity().name();
   }
}
