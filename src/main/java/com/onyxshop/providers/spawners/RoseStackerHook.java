package com.onyxshop.providers.spawners;

import dev.rosewood.rosestacker.utils.ItemUtils;
import java.util.List;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.SpawnerManager;
import com.onyxshop.providers.SpawnerProvider;
import com.onyxshop.util.NBTMaps;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class RoseStackerHook implements SpawnerProvider {
   public RoseStackerHook() {
   }

   @Override
   public String getProviderName() {
      return "ROSESTACKER";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      ItemMeta itemMeta = itemStack.getItemMeta();
      if (shopItem == null) {
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         creatureSpawner.setSpawnedType(entityType);
         blockStateMeta.setBlockState(creatureSpawner);
      }

      String type = null;
      List values = null;
      if (itemMeta.hasDisplayName()) {
         type = itemMeta.getDisplayName();
      }

      if (itemMeta.hasLore()) {
         values = itemMeta.getLore();
      }

      ItemStack itemStack2 = ItemUtils.getSpawnerAsStackedItemStack(entityType, 1);
      if (itemStack2 != null && itemStack2.hasItemMeta()) {
         if (type == null && itemStack2.getItemMeta().hasDisplayName()) {
            itemMeta.setDisplayName(itemStack2.getItemMeta().getDisplayName());
         }

         if (values == null && itemStack2.getItemMeta().hasLore()) {
            itemMeta.setLore(itemStack2.getItemMeta().getLore());
         }
      }

      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemMeta.addItemFlags(ItemFlag.values());
      itemStack.setItemMeta(itemMeta);
      return OnyxShop.getInstance().versionHandler.setNBTInt(itemStack, "StackSize", 1);
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "StackSize", "EntityType", "display::Name", "HideFlags");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      Integer nbtInt = OnyxShop.getInstance().versionHandler.getNBTInt(itemStack, NBTMaps.CUSTOM_DATA.getTag() + "StackSize");
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
