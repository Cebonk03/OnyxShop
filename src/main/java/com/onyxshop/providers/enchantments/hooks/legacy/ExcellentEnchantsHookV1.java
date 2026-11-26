package com.onyxshop.providers.enchantments.hooks.legacy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.EnchantHookLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.enchantment.impl.ExcellentEnchant;
import su.nightexpress.excellentenchants.enchantment.util.EnchantUtils;

public class ExcellentEnchantsHookV1 extends BukkitEnch implements EnchantProvider {
   private Field field;
   private Class<?> clazz;

   private Map<NamespacedKey, ExcellentEnchant> registry() {
      try {
         return (Map<NamespacedKey, ExcellentEnchant>)this.field.get(this.clazz);
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to get EnchantRegistry");
         var2.printStackTrace();
         return null;
      }
   }

   public ExcellentEnchantsHookV1() throws EnchantHookLoadException {
      try {
         this.clazz = Class.forName("su.nightexpress.excellentenchants.enchantment.EnchantRegistry");
         this.field = this.clazz.getField("REGISTRY_MAP");
      } catch (Exception var4) {
         try {
            this.clazz = Class.forName("su.nightexpress.excellentenchants.enchantment.registry.EnchantRegistry");
            this.field = this.clazz.getField("REGISTRY_MAP");
         } catch (Exception var3) {
            throw new EnchantHookLoadException("Failed to register ExcellentEnchantments hook, please update ExcellentEnchantments...");
         }
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      Enchantment var3 = Enchantment.getByKey(NamespacedKey.minecraft(var1));
      if (var3 != null) {
         return new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, var3, var1, var2);
      } else {
         SendMessage.errorMessage("ExcelentEnchants could not find a enchantment with name '" + var1 + "'");
         return null;
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack var1, SimpleEnchantment var2) {
      return super.canEnchantItem(var1, (Enchantment)var2.getEnchant());
   }

   @Override
   public ItemStack addEnchantment(ItemStack var1, SimpleEnchantment var2, @Nullable DisplayItem var3) {
      var1 = super.addEnchantment(var1, (Enchantment)var2.getEnchant(), var2.getLvl(), var3);
      EnchantUtils.updateDisplay(var1);
      return var1;
   }

   @Override
   public boolean isEnchant(NamespacedKey var1) {
      return this.registry().containsKey(var1);
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return this.registry().keySet().stream().map(var0 -> "excellentenchants:" + var0.getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return this.registry()
         .values()
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, var0, var0.getId(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return this.registry().keySet().stream().map(Enchantment::getByKey).filter(var1x -> var1x.canEnchantItem(var1)).map(var1x -> var1x).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      ItemStack var2 = super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
      EnchantUtils.updateDisplay(var2);
      return var2;
   }
}
