package com.onyxshop.providers.enchantments.hooks.legacy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.EnchantHookLoadException;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.registry.EnchantRegistry;

public class ExcelentEnchantsHookV3 extends BukkitEnch implements EnchantProvider {
   public ExcelentEnchantsHookV3() throws EnchantHookLoadException {
      try {
         Class type = Class.forName("su.nightexpress.excellentenchants.registry.EnchantRegistry");
         Field field = type.getField("BY_KEY");
      } catch (Exception exception) {
         throw new EnchantHookLoadException("Failed to register ExcellentEnchantments hook, please update ExcellentEnchantments...");
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(itemId));
      if (enchantment != null) {
         value = OnyxShop.getInstance().allowUnsafeEnchants ? value : (value > enchantment.getMaxLevel() ? enchantment.getMaxLevel() : value);
         return new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, enchantment, itemId, value);
      } else {
         throw new ItemLoadException("ExcelentEnchants could not find a enchantment with name '" + itemId + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment) {
      return super.canEnchantItem(itemStack, (Enchantment)simpleEnchantment.getEnchant());
   }

   @Override
   public ItemStack addEnchantment(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem) {
      return super.addEnchantment(itemStack, (Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl(), displayItem);
   }

   @Override
   public boolean isEnchant(NamespacedKey namespacedKey) {
      return EnchantRegistry.BY_KEY.containsKey(namespacedKey);
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return EnchantRegistry.BY_KEY.keySet().stream().map(entry -> "excellentenchants:" + entry.getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return EnchantRegistry.BY_KEY
         .values()
         .stream()
         .map(element -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, element.getBukkitEnchantment(), element.getId(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return EnchantRegistry.BY_KEY
         .keySet()
         .stream()
         .map(Enchantment::getByKey)
         .filter(element -> element.canEnchantItem(itemStack))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
   }
}
