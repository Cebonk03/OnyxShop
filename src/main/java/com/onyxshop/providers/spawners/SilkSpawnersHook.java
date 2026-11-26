package com.onyxshop.providers.spawners;

import de.dustplanet.util.SilkUtil;
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

public class SilkSpawnersHook implements SpawnerProvider {
   private final SilkUtil su;

   public SilkSpawnersHook(SilkUtil var1) {
      this.su = var1 == null ? SilkUtil.hookIntoSilkSpanwers() : var1;
   }

   @Override
   public String getProviderName() {
      return "SILKSPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      String var4 = var3.name();

      String var5;
      try {
         var5 = (String)this.su.getDisplayNameToMobID().get(var4);
      } catch (NullPointerException var7) {
         var5 = var4;
      }

      if (var5 == null) {
         var5 = var4;
      }

      ItemMeta var6 = var2.getItemMeta();
      if (!var6.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var6, SpawnerManager.getDefaultName(var3));
      }

      var2.setItemMeta(var6);
      return this.su.nmsProvider.setNBTEntityID(var2, var5);
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "display::Name");
      var4.addAll(var3);
      return OnyxShop.getInstance().isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      ItemMeta var2 = var1.getItemMeta();
      BlockStateMeta var3 = (BlockStateMeta)var2;
      CreatureSpawner var4 = (CreatureSpawner)var3.getBlockState();
      return var4.getSpawnedType().name();
   }
}
