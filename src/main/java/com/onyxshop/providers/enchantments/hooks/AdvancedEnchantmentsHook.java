package com.onyxshop.providers.enchantments.hooks;

import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.exceptions.ItemLoadException;
import net.advancedplugins.ae.api.AEAPI;
import net.advancedplugins.ae.enchanthandler.enchantments.AdvancedEnchantment;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AdvancedEnchantmentsHook implements EnchantProvider {
   public AdvancedEnchantmentsHook() {
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      if (AEAPI.isAnEnchantment(itemId)) {
         AdvancedEnchantment advancedEnchantment = AEAPI.getEnchantmentInstance(itemId);
         if (advancedEnchantment.isEnabled()) {
            value = OnyxShop.getInstance().allowUnsafeEnchants
               ? value
               : (value > AEAPI.getHighestEnchantmentLevel(itemId) ? AEAPI.getHighestEnchantmentLevel(itemId) : value);
            return new SimpleEnchantment(EnchantsProvider.Provider.ADVANCED_ENCHANTS, advancedEnchantment, itemId, value);
         } else {
            throw new ItemLoadException("Enchantment '" + itemId + "' is disabled inside AdvancedEnchantments");
         }
      } else {
         throw new ItemLoadException("AdvancedEnchantments could not find a enchantment with name '" + itemId + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment) {
      if (OnyxShop.getInstance().allowUnsafeEnchants) {
         return true;
      } else if (((AdvancedEnchantment)simpleEnchantment.getEnchant()).canBeApplied(itemStack.getType())) {
         return true;
      } else {
         SendMessage.errorMessage(
            "Could not apply enchantment '" + simpleEnchantment.getKey() + "' to item " + OnyxShop.getInstance().getMaterialName(itemStack.getType().name())
         );
         return false;
      }
   }

   @Override
   public ItemStack addEnchantment(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem) {
      return AEAPI.applyEnchant(simpleEnchantment.getKey(), simpleEnchantment.getLvl(), itemStack);
   }

   @Override
   public boolean isEnchant(NamespacedKey namespacedKey) {
      return AEAPI.isAnEnchantment(namespacedKey.getKey());
   }

   public List<String> getAppliedEnchants(ItemStack itemStack) {
      return AEAPI.getEnchantmentsOnItem(itemStack)
         .entrySet()
         .stream()
         .map(entry -> "advancedenchantments:" + (String)entry.getKey() + ":" + entry.getValue())
         .collect(Collectors.toList());
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack itemStack) {
      return AEAPI.getEnchantmentsOnItem(itemStack)
         .entrySet()
         .stream()
         .map(
            entry -> new SimpleEnchantment(
               EnchantsProvider.Provider.ADVANCED_ENCHANTS,
               AEAPI.getEnchantmentInstance((String)entry.getKey()),
               (String)entry.getKey(),
               (Integer)entry.getValue()
            )
         )
         .collect(Collectors.toList());
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return AEAPI.getAllEnchantments().stream().map(element -> "advancedenchantments:" + element + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return AEAPI.getAllEnchantments()
         .stream()
         .map(element -> new SimpleEnchantment(EnchantsProvider.Provider.ADVANCED_ENCHANTS, AEAPI.getEnchantmentInstance(element), element, 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return AEAPI.getAllEnchantments()
         .stream()
         .map(AEAPI::getEnchantmentInstance)
         .filter(element -> element.isEnabled() && element.canBeApplied(itemStack.getType()))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
      return AEAPI.applyEnchant(simpleEnchantment.getKey(), simpleEnchantment.getLvl(), itemStack);
   }

   @Override
   public String getName(SimpleEnchantment simpleEnchantment) {
      return ((AdvancedEnchantment)simpleEnchantment.getEnchant()).getName();
   }
}
