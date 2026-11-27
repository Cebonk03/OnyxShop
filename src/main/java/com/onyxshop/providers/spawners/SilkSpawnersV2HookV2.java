package com.onyxshop.providers.spawners;

import de.corneliusmay.silkspawners.api.SilkSpawnersAPI;
import de.corneliusmay.silkspawners.api.SilkSpawnersApiProvider;
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

public class SilkSpawnersV2HookV2 implements SpawnerProvider {
   private final SilkSpawnersAPI api = SilkSpawnersApiProvider.get();

   public SilkSpawnersV2HookV2() {
   }

   @Override
   public String getProviderName() {
      return "SilkSpawners_v2";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      String type = null;
      List values = null;
      if (itemStack.hasItemMeta()) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (itemMeta.hasDisplayName()) {
            type = itemMeta.getDisplayName();
         }

         if (itemMeta.hasLore()) {
            values = itemMeta.getLore();
         }
      }

      itemStack = this.api.getSpawnerItem(entityType);
      if (itemStack.hasItemMeta()) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         if (type != null) {
            itemMeta.setDisplayName(type);
         }

         if (!itemMeta.hasDisplayName()) {
            OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
         }

         if (values != null) {
            itemMeta.setLore(values);
         }

         itemStack.setItemMeta(itemMeta);
      }

      return itemStack;
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "display::Lore", "display::Name", "HideFlags");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      EntityType entityType = this.api.getEntityType(itemStack);
      if (entityType == null) {
         ItemMeta itemMeta = itemStack.getItemMeta();
         BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
         CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
         entityType = creatureSpawner.getSpawnedType();
      }

      return entityType.name();
   }
}
