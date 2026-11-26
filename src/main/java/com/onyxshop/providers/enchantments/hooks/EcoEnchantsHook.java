package com.onyxshop.providers.enchantments.hooks;

import com.willfp.ecoenchants.enchant.EcoEnchant;
import com.willfp.ecoenchants.enchant.EcoEnchants;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.EnchantHookLoadException;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EcoEnchantsHook extends BukkitEnch implements EnchantProvider {
   private Field field;
   private Class<?> clazz;

   private Map<String, EcoEnchant> registry() {
      try {
         return (Map<String, EcoEnchant>)this.field.get(this.clazz);
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to get EnchantRegistry");
         var2.printStackTrace();
         return null;
      }
   }

   public EcoEnchantsHook() throws EnchantHookLoadException {
      try {
         this.clazz = Class.forName("com.willfp.ecoenchants.enchant.EcoEnchants");
         this.field = this.clazz.getDeclaredField("BY_NAME");
         this.field.setAccessible(true);
      } catch (Exception var2) {
         throw new EnchantHookLoadException(
            "Failed to register EcoEnchants hook, this version of EcoEnchants("
               + Bukkit.getPluginManager().getPlugin("EcoEnchants").getDescription().getVersion()
               + ") is not supported..."
         );
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      Enchantment var3 = Enchantment.getByKey(NamespacedKey.minecraft(var1));
      if (var3 != null) {
         var2 = OnyxShop.getInstance().allowUnsafeEnchants ? var2 : (var2 > var3.getMaxLevel() ? var3.getMaxLevel() : var2);
         return new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, var3, var1, var2);
      } else {
         throw new ItemLoadException("EcoEnchants could not find a enchantment with name '" + var1 + "'");
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
      return EcoEnchants.INSTANCE.getByID(var1.getKey()) != null;
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return this.registry().values().stream().map(var0 -> "ecoenchants:" + var0.getEnchantmentKey().getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return this.registry()
         .values()
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, var0.getEnchantment(), var0.getEnchantmentKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return this.registry().values().stream().filter(var1x -> var1x.canEnchantItem(var1)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      return super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
   }
}
