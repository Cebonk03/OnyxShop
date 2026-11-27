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
      } catch (Exception exception) {
         SendMessage.warnMessage("Failed to get EnchantRegistry");
         exception.printStackTrace();
         return null;
      }
   }

   public EcoEnchantsHook() throws EnchantHookLoadException {
      try {
         this.clazz = Class.forName("com.willfp.ecoenchants.enchant.EcoEnchants");
         this.field = this.clazz.getDeclaredField("BY_NAME");
         this.field.setAccessible(true);
      } catch (Exception exception) {
         throw new EnchantHookLoadException(
            "Failed to register EcoEnchants hook, this version of EcoEnchants("
               + Bukkit.getPluginManager().getPlugin("EcoEnchants").getDescription().getVersion()
               + ") is not supported..."
         );
      }
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(itemId));
      if (enchantment != null) {
         value = OnyxShop.getInstance().allowUnsafeEnchants ? value : (value > enchantment.getMaxLevel() ? enchantment.getMaxLevel() : value);
         return new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, enchantment, itemId, value);
      } else {
         throw new ItemLoadException("EcoEnchants could not find a enchantment with name '" + itemId + "'");
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
      return EcoEnchants.INSTANCE.getByID(namespacedKey.getKey()) != null;
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return this.registry().values().stream().map(element -> "ecoenchants:" + element.getEnchantmentKey().getKey() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return this.registry()
         .values()
         .stream()
         .map(element -> new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, element.getEnchantment(), element.getEnchantmentKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return this.registry().values().stream().filter(element -> element.canEnchantItem(itemStack)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
   }
}
