package com.onyxshop.providers;

import java.util.List;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface EnchantProvider {
   SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException;

   boolean canEnchantItem(ItemStack var1, SimpleEnchantment var2);

   ItemStack addEnchantment(ItemStack var1, SimpleEnchantment var2, @Nullable DisplayItem var3);

   boolean isEnchant(NamespacedKey var1);

   List<String> getAvailableEnchantKeys();

   List<SimpleEnchantment> getAvailableEnchants();

   List<Object> getAvailableEnchants(ItemStack var1);

   ItemStack getBook(SimpleEnchantment var1);

   String getName(SimpleEnchantment var1);
}
