package com.onyxshop.providers.enchantments.hooks;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.BukkitEnch;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.XEnchantment;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class VanillaEnchant extends BukkitEnch implements EnchantProvider {
   public VanillaEnchant() {
   }

   @Override
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      Optional var3 = XEnchantment.matchXEnchantment(var1);
      if (var3.isPresent()) {
         if (XEnchantment.matchXEnchantment(var1).get().isSupported()) {
            Enchantment var4 = XEnchantment.matchXEnchantment(var1).get().parseEnchantment();
            var2 = OnyxShop.getInstance().allowUnsafeEnchants ? var2 : (var2 > var4.getMaxLevel() ? var4.getMaxLevel() : var2);
            return new SimpleEnchantment(EnchantsProvider.Provider.VANILLA, var4, var1, var2);
         } else {
            throw new ItemLoadException(Lang.ITEM_ENCHANTMENT_NOT_SUPPORTED.get());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_ENCHANTMENT_NULL.get());
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
      try {
         return XEnchantment.matchXEnchantment(var1.getKey()).get().parseEnchantment() != null;
      } catch (NullPointerException | NoSuchElementException var3) {
         return false;
      }
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return XEnchantment.getNames();
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return OnyxShop.getInstance()
         .supportedEnchants
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.VANILLA, var0, var0.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return OnyxShop.getInstance().supportedEnchants.stream().filter(var1x -> var1x.canEnchantItem(var1)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      return super.getBook((Enchantment)var1.getEnchant(), var1.getLvl());
   }
}
