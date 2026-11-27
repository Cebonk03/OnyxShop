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
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (shopItem == null) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         creatureSpawner.setSpawnedType(entityType);
         blockStateMeta.setBlockState(creatureSpawner);
         itemStack = OnyxShop.getInstance().versionHandler.setNBTString(itemStack, "ms_mob", entityType.name());
      }

      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "ms_mob", "display::Lore", "display::Name");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      return OnyxShop.getInstance().versionHandler.getSpawnerType(itemStack);
   }
}
