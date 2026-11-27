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

   public SilkSpawnersHook(SilkUtil silkUtil) {
      this.su = silkUtil == null ? SilkUtil.hookIntoSilkSpanwers() : silkUtil;
   }

   @Override
   public String getProviderName() {
      return "SILKSPAWNERS";
   }

   @Override
   public ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException {
      String type = entityType.name();

      String text;
      try {
         text = (String)this.su.getDisplayNameToMobID().get(type);
      } catch (NullPointerException exception) {
         text = type;
      }

      if (text == null) {
         text = type;
      }

      ItemMeta itemMeta = itemStack.getItemMeta();
      if (!itemMeta.hasDisplayName()) {
         OnyxShop.getInstance().getMetaUtils().setDisplayName(itemMeta, SpawnerManager.getDefaultName(entityType));
      }

      itemStack.setItemMeta(itemMeta);
      return this.su.nmsProvider.setNBTEntityID(itemStack, text);
   }

   @Override
   public boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds) {
      List tags = NBTMaps.getTags(false, "display::Name");
      tags.addAll(shopIds);
      return OnyxShop.getInstance().isSimilar(itemStack, itemStack2, tags);
   }

   @Override
   public Double getSpawnerSellPrice(ItemStack itemStack, double price) {
      return price;
   }

   @Override
   public String getSpawnedType(ItemStack itemStack) {
      ItemMeta itemMeta = itemStack.getItemMeta();
      BlockStateMeta blockStateMeta = (BlockStateMeta)itemMeta;
      CreatureSpawner creatureSpawner = (CreatureSpawner)blockStateMeta.getBlockState();
      return creatureSpawner.getSpawnedType().name();
   }
}
