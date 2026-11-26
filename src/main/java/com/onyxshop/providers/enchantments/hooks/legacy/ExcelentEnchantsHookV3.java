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
         Class var1 = Class.forName("su.nightexpress.excellentenchants.registry.EnchantRegistry");
         Field var2 = var1.getField("BY_KEY");
      } catch (Exception var3) {
         throw new EnchantHookLoadException("Failed to register ExcellentEnchantments hook, please update ExcellentEnchantments...");
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      Enchantment var3 = Enchantment.getByKey(NamespacedKey.minecraft(var1));
      if (var3 != null) {
         var2 = OnyxShop.getInstance().allowUnsafeEnchants ? var2 : (var2 > var3.getMaxLevel() ? var3.getMaxLevel() : var2);
         return new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, var3, var1, var2);
      } else {
         throw new ItemLoadException("ExcelentEnchants could not find a enchantment with name '" + var1 + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack var1, SimpleEnchantment var2) {
      return super.canEnchantItem(var1, (Enchantment)var2.getEnchant());
   }

   @Override
   public ItemStack addEnchantment(ItemStack var1, SimpleEnchantment var2, @Nullable DisplayItem var3) {
      return super.addEnchantment(var1, (Enchantment)var2.getEnchant(), var2.getLvl(), var3);
   }

   @Override
   public boolean isEnchant(NamespacedKey var1) {
      return EnchantRegistry.BY_KEY.containsKey(var1);
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return EnchantRegistry.BY_KEY.keySet().stream().map(var0 -> "excellentenchants:" + var0.getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return EnchantRegistry.BY_KEY
         .values()
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, var0.getBukkitEnchantment(), var0.getId(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return EnchantRegistry.BY_KEY
         .keySet()
         .stream()
         .map(Enchantment::getByKey)
         .filter(var1x -> var1x.canEnchantItem(var1))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      return super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
   }
}
