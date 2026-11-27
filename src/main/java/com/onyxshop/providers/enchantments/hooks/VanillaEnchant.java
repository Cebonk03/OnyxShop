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
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      Optional optional = XEnchantment.matchXEnchantment(itemId);
      if (optional.isPresent()) {
         if (XEnchantment.matchXEnchantment(itemId).get().isSupported()) {
            Enchantment enchantment = XEnchantment.matchXEnchantment(itemId).get().parseEnchantment();
            value = OnyxShop.getInstance().allowUnsafeEnchants ? value : (value > enchantment.getMaxLevel() ? enchantment.getMaxLevel() : value);
            return new SimpleEnchantment(EnchantsProvider.Provider.VANILLA, enchantment, itemId, value);
         } else {
            throw new ItemLoadException(Lang.ITEM_ENCHANTMENT_NOT_SUPPORTED.get());
         }
      } else {
         throw new ItemLoadException(Lang.ITEM_ENCHANTMENT_NULL.get());
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
      try {
         return XEnchantment.matchXEnchantment(namespacedKey.getKey()).get().parseEnchantment() != null;
      } catch (NullPointerException | NoSuchElementException exception) {
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
         .map(entry -> new SimpleEnchantment(EnchantsProvider.Provider.VANILLA, entry, entry.getKey().getKey(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return OnyxShop.getInstance().supportedEnchants.stream().filter(element -> element.canEnchantItem(itemStack)).collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      return super.getBook((Enchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
   }
}
