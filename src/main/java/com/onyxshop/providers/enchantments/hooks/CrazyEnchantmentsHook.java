package com.onyxshop.providers.enchantments.hooks;

import com.badbones69.crazyenchantments.paper.CrazyEnchantments;
import com.badbones69.crazyenchantments.paper.Starter;
import com.badbones69.crazyenchantments.paper.api.enums.CEnchantments;
import com.badbones69.crazyenchantments.paper.api.objects.CEBook;
import com.badbones69.crazyenchantments.paper.api.objects.CEnchantment;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantProvider;
import com.onyxshop.providers.EnchantsProvider;
import com.onyxshop.providers.enchantments.SimpleEnchantment;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CrazyEnchantmentsHook implements EnchantProvider {
   private final Starter ce = ((CrazyEnchantments)Bukkit.getPluginManager().getPlugin("CrazyEnchantments")).getStarter();

   public CrazyEnchantmentsHook() {
   }

   @Override
   public String getName(SimpleEnchantment simpleEnchantment) {
      return ((CEnchantment)simpleEnchantment.getEnchant()).getName();
   }

   @Override
   public SimpleEnchantment getEnchant(String itemId, int value) throws ItemLoadException {
      CEnchantment cEnchantment = this.ce.getCrazyManager().getEnchantmentFromName(itemId);
      if (cEnchantment != null) {
         value = OnyxShop.getInstance().allowUnsafeEnchants ? value : (value > cEnchantment.getMaxLevel() ? cEnchantment.getMaxLevel() : value);
         return new SimpleEnchantment(EnchantsProvider.Provider.CRAZY_ENCHANTS, cEnchantment, itemId, value);
      } else {
         throw new ItemLoadException("CrazyEnchantments could not find a enchantment with name '" + itemId + "'");
      }
   }

   @Override
   public boolean canEnchantItem(ItemStack itemStack, SimpleEnchantment simpleEnchantment) {
      if (OnyxShop.getInstance().allowUnsafeEnchants) {
         return true;
      } else {
         if (simpleEnchantment.getEnchant() instanceof CEnchantment) {
            CEnchantment cEnchantment = (CEnchantment)simpleEnchantment.getEnchant();
            if (!cEnchantment.canEnchantItem(itemStack)) {
               SendMessage.errorMessage("Could not apply enchantment '" + simpleEnchantment.getKey() + "' to this shop item material type");
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public ItemStack addEnchantment(ItemStack itemStack, SimpleEnchantment simpleEnchantment, @Nullable DisplayItem displayItem) {
      if (simpleEnchantment.getEnchant() instanceof CEnchantment) {
         if (XMaterial.matchXMaterial(itemStack) == XMaterial.BOOK) {
            CEBook ceBook = new CEBook((CEnchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl(), itemStack.getAmount());
            ceBook.setSuccessRate(100);
            ceBook.setDestroyRate(0);
            ItemBuilder itemBuilder = new ItemBuilder(ceBook.buildBook());
            itemBuilder.withSimpleMeta(itemStack);
            return itemBuilder.build();
         } else {
            return this.ce.getCrazyManager().addEnchantment(itemStack, (CEnchantment)simpleEnchantment.getEnchant(), simpleEnchantment.getLvl());
         }
      } else {
         return itemStack;
      }
   }

   @Override
   public boolean isEnchant(NamespacedKey namespacedKey) {
      return this.ce.getCrazyManager().getEnchantmentFromName(namespacedKey.getKey()) != null;
   }

   public List<String> getAppliedEnchants(ItemStack itemStack) {
      return this.ce
         .getEnchantmentBookSettings()
         .getEnchantments(itemStack)
         .entrySet()
         .stream()
         .map(entry -> "crazyenchantments:" + ((CEnchantment)entry.getKey()).getName() + ":" + entry.getValue())
         .collect(Collectors.toList());
   }

   public List<SimpleEnchantment> getEnchantsOfItem(ItemStack itemStack) {
      return this.ce
         .getEnchantmentBookSettings()
         .getEnchantments(itemStack)
         .entrySet()
         .stream()
         .map(
            entry -> new SimpleEnchantment(
               EnchantsProvider.Provider.CRAZY_ENCHANTS, entry.getKey(), ((CEnchantment)entry.getKey()).getName(), (Integer)entry.getValue()
            )
         )
         .collect(Collectors.toList());
   }

   @Override
   public List<String> getAvailableEnchantKeys() {
      return Arrays.stream(CEnchantments.values()).map(element -> "crazyenchantments:" + element.getName() + ":1").collect(Collectors.toList());
   }

   @Override
   public List<SimpleEnchantment> getAvailableEnchants() {
      return Arrays.stream(CEnchantments.values())
         .map(element -> new SimpleEnchantment(EnchantsProvider.Provider.CRAZY_ENCHANTS, element.getEnchantment(), element.getName(), 1))
         .collect(Collectors.toList());
   }

   @Override
   public List<Object> getAvailableEnchants(ItemStack itemStack) {
      return Arrays.stream(CEnchantments.values())
         .map(CEnchantments::getEnchantment)
         .filter(element -> element.canEnchantItem(itemStack))
         .collect(Collectors.toList());
   }

   @Override
   public ItemStack getBook(SimpleEnchantment simpleEnchantment) {
      ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
      itemStack = this.addEnchantment(itemStack, simpleEnchantment, null);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setDisplayName(((CEnchantment)simpleEnchantment.getEnchant()).getInfoName());
      itemMeta.setLore(((CEnchantment)simpleEnchantment.getEnchant()).getInfoDescription());
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }
}
