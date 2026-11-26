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
      File var1 = new File(OnyxShop.getInstance().getDataFolder().getParent() + "/EcoEnchants/", "config.yml");
      if (var1.exists()) {
         YamlConfiguration var2 = OnyxShop.getInstance().loadConfiguration(var1, "/EcoEnchants/config.yml");
         this.descriptions = var2.getBoolean("display.descriptions.enabled", true);
      } else {
         this.descriptions = true;
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
      return EcoEnchants.getByKey(var1) != null;
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return EcoEnchants.keySet().stream().map(var0 -> "ecoenchants:" + var0 + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return EcoEnchants.values()
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.ECO_ENCHANTS, var0, var0.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return EcoEnchants.values().stream().filter(var1x -> var1x.canEnchantItem(var1)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      return super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
   }
}
