package com.onyxshop.providers;

import java.util.List;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface SpawnerProvider {
   String getProviderName();

   ItemStack setSpawnedType(@Nullable ShopItem shopItem, ItemStack itemStack, EntityType entityType) throws ItemLoadException;

   boolean isShopSpawner(ItemStack itemStack, ItemStack itemStack2, List<String> shopIds);

   Double getSpawnerSellPrice(ItemStack itemStack, double price);

   String getSpawnedType(ItemStack itemStack);
}
