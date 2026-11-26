package com.onyxshop.providers.enchantments.hooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.methodes.UpdateChecker;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.exceptions.EnchantHookLoadException;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;

public class ExcellentEnchantsHook extends BukkitEnch implements EnchantProvider {
   private final Class<?> registry;

   public ExcellentEnchantsHook(UpdateChecker.Version var1) throws EnchantHookLoadException {
      String var2 = !var1.isGreater(new UpdateChecker.Version("5.3.0")) && !var1.isSame(new UpdateChecker.Version("5.3.0")) ? "api" : "enchantment";

      try {
         this.registry = Class.forName("su.nightexpress.excellentenchants." + var2 + ".EnchantRegistry");
      } catch (ClassNotFoundException var4) {
         throw new EnchantHookLoadException("Failed to locate enchantment registry class for ExcellentEnchantments, this is a bug.");
      }
   }

   private Set<CustomEnchantment> getRegistered() {
      try {
         return (Set<CustomEnchantment>)ReflectUtil.invokeStaticLegacyMethod("getRegistered", this.registry, null, null);
      } catch (Exception var2) {
         SendMessage.warnMessage("Failed to retrieve registered enchantments from ExcellentEnchants");
         var2.printStackTrace();
         return new HashSet<>();
      }
   }

   private CustomEnchantment getByKey(NamespacedKey var1) {
      try {
         return (CustomEnchantment)ReflectUtil.invokeStaticLegacyMethod("getByKey", this.registry, new Class[]{NamespacedKey.class}, var1);
      } catch (Exception var3) {
         SendMessage.warnMessage("Failed to retrieve registered enchantments from ExcellentEnchants");
         var3.printStackTrace();
         return null;
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      Enchantment var3 = Enchantment.getByKey(new NamespacedKey("excellentenchants", var1));
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
      return Registry.ENCHANTMENT
         .stream()
         .filter(var0 -> var0.getKey().getNamespace().equals("excellentenchants"))
         .anyMatch(var1x -> var1x.getKey().equals(var1));
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return Registry.ENCHANTMENT
         .stream()
         .filter(var0 -> var0.getKey().getNamespace().equals("excellentenchants"))
         .map(var0 -> "excellentenchants:" + var0.getKey().getKey() + ":1")
         .collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return Registry.ENCHANTMENT
         .stream()
         .filter(var0 -> var0.getKey().getNamespace().equals("excellentenchants"))
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, var0, var0.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return Registry.ENCHANTMENT
         .stream()
         .filter(var1x -> var1x.getKey().getNamespace().equals("excellentenchants") && var1x.canEnchantItem(var1))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      return super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
   }
}
