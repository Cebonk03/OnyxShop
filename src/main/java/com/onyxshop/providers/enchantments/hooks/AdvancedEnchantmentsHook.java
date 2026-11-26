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
   public SimpleEnchantment getEnchant(String var1, int var2) throws ItemLoadException {
      if (AEAPI.isAnEnchantment(var1)) {
         AdvancedEnchantment var3 = AEAPI.getEnchantmentInstance(var1);
         if (var3.isEnabled()) {
            var2 = OnyxShop.getInstance().allowUnsafeEnchants
               ? var2
               : (var2 > AEAPI.getHighestEnchantmentLevel(var1) ? AEAPI.getHighestEnchantmentLevel(var1) : var2);
            return new SimpleEnchantment(EnchantsProvider.Provider.ADVANCED_ENCHANTS, var3, var1, var2);
         } else {
            throw new ItemLoadException("Enchantment '" + var1 + "' is disabled inside AdvancedEnchantments");
         }
      } else {
         throw new ItemLoadException("AdvancedEnchantments could not find a enchantment with name '" + var1 + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack var1, SimpleEnchantment var2) {
      if (OnyxShop.getInstance().allowUnsafeEnchants) {
         return true;
      } else if (((AdvancedEnchantment)var2.getEnchant()).canBeApplied(var1.getType())) {
         return true;
      } else {
         SendMessage.errorMessage(
            "Could not apply enchantment '" + var2.getKey() + "' to item " + OnyxShop.getInstance().getMaterialName(var1.getType().name())
         );
         return false;
      }
   }

   @Override
   public ItemStack addEnchantment(ItemStack var1, SimpleEnchantment var2, @Nullable DisplayItem var3) {
      return AEAPI.applyEnchant(var2.getKey(), var2.getLvl(), var1);
   }

   @Override
   public boolean isEnchant(NamespacedKey var1) {
      return AEAPI.isAnEnchantment(var1.getKey());
   }

   public List<String> getAppliedEnchants(ItemStack var1) {
      return AEAPI.getEnchantmentsOnItem(var1)
         .entrySet()
         .stream()
         .map(var0 -> "advancedenchantments:" + (String)var0.getKey() + ":" + var0.getValue())
         .collect(Collectors.toList());
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack var1) {
      return AEAPI.getEnchantmentsOnItem(var1)
         .entrySet()
         .stream()
         .map(
            var0 -> new SimpleEnchantment(
               EnchantsProvider.Provider.ADVANCED_ENCHANTS,
               AEAPI.getEnchantmentInstance((String)var0.getKey()),
               (String)var0.getKey(),
               (Integer)var0.getValue()
            )
         )
         .collect(Collectors.toList());
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return AEAPI.getAllEnchantments().stream().map(var0 -> "advancedenchantments:" + var0 + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return AEAPI.getAllEnchantments()
         .stream()
         .map(var0 -> new SimpleEnchantment(EnchantsProvider.Provider.ADVANCED_ENCHANTS, AEAPI.getEnchantmentInstance(var0), var0, 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack var1) {
      return AEAPI.getAllEnchantments()
         .stream()
         .map(AEAPI::getEnchantmentInstance)
         .filter(var1x -> var1x.isEnabled() && var1x.canBeApplied(var1.getType()))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment var1) {
      ItemStack var2 = new ItemStack(Material.ENCHANTED_BOOK);
      return AEAPI.applyEnchant(var1.getKey(), var1.getLvl(), var2);
   }

   @Override
   public String getName(SimpleEnchantment var1) {
      return ((AdvancedEnchantment)var1.getEnchant()).getName();
   }
}
