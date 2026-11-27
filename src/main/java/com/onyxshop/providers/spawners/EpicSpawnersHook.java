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

   public EpicSpawnersHook(OnyxShop plugin) {
      this.plugin = plugin;
   }

   @Override
   public String getProviderName() {
      return "EPICSPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      itemStack = this.plugin.versionHandler.setNBTInt(itemStack, "size", 1);
      itemStack = this.plugin.versionHandler.setNBTString(itemStack, "data", entityType.name());
      itemStack = this.plugin.versionHandler.setNBTString(itemStack, "tier", "Tier_1");
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "size", "tier", "display::Name");
      tags.addAll(shopIds);
      return this.plugin.isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      Integer nbtInt = this.plugin.versionHandler.getNBTInt(itemStack, NBTMaps.CUSTOM_DATA.getTag() + "size");
      nbtInt = nbtInt == null ? 1 : nbtInt;
      return nbtInt.intValue() * price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      return this.plugin.versionHandler.getNBTString(itemStack, "data");
   }
}
