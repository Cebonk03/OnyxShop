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
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to get EnchantRegistry");
         exception.printStackTrace();
         return null;
      }
   }

   public ExcellentEnchantsHookV1() throws EnchantHookLoadException {
      try {
         this.clazz = Class.forName("su.nightexpress.excellentenchants.enchantment.EnchantRegistry");
         this.field = this.clazz.getField("REGISTRY_MAP");
      } catch (Exception exception) {
         try {
            this.clazz = Class.forName("su.nightexpress.excellentenchants.enchantment.registry.EnchantRegistry");
            this.field = this.clazz.getField("REGISTRY_MAP");
         } catch (Exception exception2) {
            throw new EnchantHookLoadException("Failed to register ExcellentEnchantments hook, please update ExcellentEnchantments...");
         }
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(itemId));
      if (enchantment != null) {
         return new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, enchantment, itemId, value);
      } else {
         SendMessage.errorMessage("ExcelentEnchants could not find a enchantment with name '" + itemId + "'");
         return null;
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment) {
      return super.canEnchantItem(itemStack, (Enchantment)simpleEnchantment.getEnchant());
   }

   @Override
   public ItemStack addEnchantment(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem) {
      itemStack = super.addEnchantment(itemStack, (Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl(), displayItem);
      EnchantUtils.updateDisplay(itemStack);
      return itemStack;
   }

   @Override
   public boolean isEnchant(NamespacedKey namespacedKey) {
      return this.registry().containsKey(namespacedKey);
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return this.registry().keySet().stream().map(entry -> "excellentenchants:" + entry.getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return this.registry()
         .values()
         .stream()
         .map(element -> new SimpleEnchantment(EnchantsProvider.Provider.EXCELLENT_ENCHANTS, element, element.getId(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return this.registry().keySet().stream().map(Enchantment::getByKey).filter(element -> element.canEnchantItem(itemStack)).map(element -> element).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      ItemStack itemStack = super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
      EnchantUtils.updateDisplay(itemStack);
      return itemStack;
   }
}
