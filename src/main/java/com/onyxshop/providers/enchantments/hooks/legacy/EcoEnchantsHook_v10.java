package com.onyxshop.providers.enchantments.hooks.legacy;

import com.willfp.ecoenchants.enchants.EcoEnchants;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class EcoEnchantsHook_v10 extends BukkitEnch implements EnchantProvider {
   private final boolean descriptions;

   public EcoEnchantsHook_v10() {
      File file = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/EcoEnchants/", "config.yml");
      if (file.exists()) {
         YamlConfiguration config = OnyxShop.getInstance().loadConfiguration(file, "/EcoEnchants/config.yml");
         this.descriptions = config.getBoolean("display.descriptions.enabled", true);
      } else {
         this.descriptions = true;
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
      return EcoEnchants.getByKey(namespacedKey) != null;
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return EcoEnchants.keySet().stream().map(element -> "ecoenchants:" + element + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return EcoEnchants.values()
         .stream()
         .map(entry -> new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, entry, entry.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return EcoEnchants.values().stream().filter(element -> element.canEnchantItem(itemStack)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
   }
}
