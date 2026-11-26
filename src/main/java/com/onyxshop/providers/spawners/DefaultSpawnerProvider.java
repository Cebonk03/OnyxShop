package com.onyxshop.providers.spawners;

import java.util.List;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.SpawnerBreakEvent;
import com.onyxshop.events.SpawnerInteractEvent;
import com.onyxshop.events.SpawnerPlaceEvent;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.NBTMaps;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class DefaultSpawnerProvider implements SpawnerProvider {
   private final OnyxShop plugin;
   private final boolean interact;

   public DefaultSpawnerProvider(OnyxShop var1, @Nullable SpawnerInteractEvent var2, @Nullable SpawnerPlaceEvent var3, @Nullable SpawnerBreakEvent var4) {
      this.plugin = var1;
      if (var3 != null) {
         this.plugin.getServer().getPluginManager().registerEvents(var3, this.plugin);
      }

      if (var4 != null) {
         this.plugin.getServer().getPluginManager().registerEvents(var4, this.plugin);
      }

      if (var2 != null) {
         this.plugin.getServer().getPluginManager().registerEvents(var2, this.plugin);
      }

      this.interact = var2 == null;
   }

   @Override
   public String getProviderName() {
      return "DEFAULT";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException {
      if (var1 == null) {
         var2 = this.plugin.versionHandler.setSpawnerType(var2, var3.name());
      }

      ItemMeta var4 = var2.getItemMeta();
      if (!this.interact) {
         var2 = new ItemBuilder(var2).hideEntityData().build();
      }

      if (!var4.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(var4, SpawnerManager.getDefaultName(var3));
      }

      var2.setItemMeta(var4);
      return var2;
   }

   @Override
   public boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3) {
      List var4 = NBTMaps.getTags(false, "display::Name");
      var4.addAll(var3);
      return this.plugin.isSimilar(var1, var2, var4);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack var1, double var2) {
      return var2;
   }

   @Override
   public String getSpawnedType(ItemStack var1) {
      return this.plugin.versionHandler.getSpawnerType(var1);
   }
}
