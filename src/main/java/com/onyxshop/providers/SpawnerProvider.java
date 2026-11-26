package com.onyxshop.providers;

import java.util.List;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface SpawnerProvider {
   String getProviderName();

   ItemStack setSpawnedType(@Nullable ShopItem var1, ItemStack var2, EntityType var3) throws ItemLoadException;

   boolean isShopSpawner(ItemStack var1, ItemStack var2, List<String> var3);

   Double getSpawnerSellPrice(ItemStack var1, double var2);

   String getSpawnedType(ItemStack var1);
}
