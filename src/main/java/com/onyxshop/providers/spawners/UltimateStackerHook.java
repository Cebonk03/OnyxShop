package com.onyxshop.providers.spawners;

import java.util.List;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.NBTMaps;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class UltimateStackerHook implements SpawnerProvider {
   public UltimateStackerHook() {
   }

   @Override
   public String getProviderName() {
      return "ULTIMATESTACKER";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      ItemMeta var4 = var2.getItemMeta();
      if (var1 == null) {
         BlockStateMeta var5 = (BlockStateMeta)var4;
         CreatureSpawner var6 = (CreatureSpawner)var5.getBlockState();
         var6.setSpawnedType(var3);
         var5.setBlockState(var6);
         var2 = OnyxShop.getInstance().versionHandler.setNBTInt(var2, "spawner_stack_size", 1);
      }

      if (!var4.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
      }

      var2.setItemMeta(var4);
      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "spawner_stack_size", "display::Name");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      Integer var4 = OnyxShop.getInstance().versionHandler.getNBTInt(var1, NBTMaps.CUSTOM_DATA.getTag() + "spawner_stack_size");
      var4 = var4 == null ? 1 : var4;
      return var4.intValue() * var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      ItemMeta var2 = var1.getItemMeta();
      BlockStateMeta var3 = (BlockStateMeta)var2;
      CreatureSpawner var4 = (CreatureSpawner)var3.getBlockState();
      return var4.getSpawnedType().name();
   }
}
