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

   public ExcellentEnchantsHook(UpdateChecker.Version version) throws EnchantHookLoadException {
      String text = !version.isGreater(new UpdateChecker.Version("5.3.0")) && !version.isSame(new UpdateChecker.Version("5.3.0")) ? "api" : "enchantment";

      try {
         this.registry = Class.forName("su.nightexpress.excellentenchants." + text + ".EnchantRegistry");
      } catch (ClassNotFoundException exception) {
         throw new EnchantHookLoadException("Failed to locate enchantment registry class for ExcellentEnchantments, this is a bug.");
      }
   }

   private Set<CustomEnchantment> getRegistered() {
      try {
         return (Set<CustomEnchantment>)ReflectUtil.invokeStaticLegacyMethod("getRegistered", this.registry, null, null);
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to retrieve registered enchantments from ExcellentEnchants");
         exception.printStackTrace();
         return new HashSet<>();
      }
   }

   private CustomEnchantment getByKey(NamespacedKey namespacedKey) {
      try {
         return (CustomEnchantment)ReflectUtil.invokeStaticLegacyMethod("getByKey", this.registry, new Class[]{NamespacedKey.class}, namespacedKey);
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to retrieve registered enchantments from ExcellentEnchants");
         exception.printStackTrace();
         return null;
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      Enchantment enchantment = Enchantment.getByKey(new NamespacedKey("excellentenchants", itemId));
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
      return Registry.ENCHANTMENT
         .stream()
         .filter(entry -> entry.getKey().getNamespace().equals("excellentenchants"))
         .anyMatch(entry -> entry.getKey().equals(namespacedKey));
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return Registry.ENCHANTMENT
         .stream()
         .filter(entry -> entry.getKey().getNamespace().equals("excellentenchants"))
         .map(entry -> "excellentenchants:" + entry.getKey().getKey() + ":1")
         .collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return Registry.ENCHANTMENT
         .stream()
         .filter(entry -> entry.getKey().getNamespace().equals("excellentenchants"))
         .map(entry -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, entry, entry.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return Registry.ENCHANTMENT
         .stream()
         .filter(entry -> entry.getKey().getNamespace().equals("excellentenchants") && entry.canEnchantItem(itemStack))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
   }
}
