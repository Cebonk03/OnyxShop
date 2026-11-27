package com.onyxshop.providers;

import java.util.List;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface EnchantProvider {
   SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException;

   boolean canEnchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment);

   ItemStack addEnchantment(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem);

   boolean isEnchant(NamespacedKey namespacedKey);

   List<String> getAvailableEnchantKeys();

   List<SimpleEnchantment> getAvailableEnchants();

   List<Object> getAvailableEnchants(ItemStack itemStack);

   ItemStack getBook(SimpleEnchantment simpleEnchantment);

   String getName(SimpleEnchantment simpleEnchantment);
}
