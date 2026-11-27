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

   public DefaultSpawnerProvider(OnyxShop plugin, @Nullable SpawnerInteractEvent spawnerInteractEvent, @Nullable SpawnerPlaceEvent spawnerPlaceEvent, @Nullable SpawnerBreakEvent spawnerBreakEvent) {
      this.plugin = plugin;
      if (spawnerPlaceEvent != null) {
         this.plugin.getServer().getPluginManager().registerEvents(spawnerPlaceEvent, this.plugin);
      }

      if (spawnerBreakEvent != null) {
         this.plugin.getServer().getPluginManager().registerEvents(spawnerBreakEvent, this.plugin);
      }

      if (spawnerInteractEvent != null) {
         this.plugin.getServer().getPluginManager().registerEvents(spawnerInteractEvent, this.plugin);
      }

      this.interact = spawnerInteractEvent == null;
   }

   @Override
   public String getProviderName() {
      return "DEFAULT";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      if (shopItem == null) {
         itemStack = this.plugin.versionHandler.setSpawnerType(itemStack, entityType.name());
      }

      ItemMeta itemMeta = itemStack.getItemMeta();
      if (!this.interact) {
         itemStack = new ItemBuilder(itemStack).hideEntityData().build();
      }

      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "display::Name");
      tags.addAll(shopIds);
      return this.plugin.isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      return this.plugin.versionHandler.getSpawnerType(itemStack);
   }
}
