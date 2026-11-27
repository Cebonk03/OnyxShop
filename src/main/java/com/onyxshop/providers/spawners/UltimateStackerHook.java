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
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (shopItem == null) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         creatureSpawner.setSpawnedType(entityType);
         blockStateMeta.setBlockState(creatureSpawner);
         itemStack = OnyxShop.getInstance().versionHandler.setNBTInt(itemStack, "spawner_stack_size", 1);
      }

      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "spawner_stack_size", "display::Name");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      Integer nbtInt = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.CUSTOM_DATA.getTag() + "spawner_stack_size");
      nbtInt = nbtInt == null ? 1 : nbtInt;
      return nbtInt.intValue() * price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
      CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
      return creatureSpawner.getSpawnedType().name();
   }
}
