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

public class MineableSpawnersHook implements SpawnerProvider {
   public MineableSpawnersHook() {
   }

   @Override
   public String getProviderName() {
      return "MINEABLESPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      ItemMeta var4 = var2.getItemMeta();
      if (var1 == null) {
         BlockStateMeta var5 = (BlockStateMeta)var4;
         CreatureSpawner var6 = (CreatureSpawner)var5.getBlockState();
         var6.setSpawnedType(var3);
         var5.setBlockState(var6);
         var2 = OnyxShop.getInstance().versionHandler.setNBTString(var2, "ms_mob", var3.name());
      }

      if (!var4.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
      }

      var2.setItemMeta(var4);
      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "ms_mob", "display::Lore", "display::Name");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      return OnyxShop.getInstance().versionHandler.getSpawnerType(var1);
   }
}
