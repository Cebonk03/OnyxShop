package com.onyxshop.providers.enchantments;

import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.util.XMaterial;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.Nullable;

public class BukkitEnch {
   public BukkitEnch() {
   }

   public String getName(SimpleEnchantment simpleEnchantment) {
      return ((Enchantment)simpleEnchantment.getEnchant()).getName();
   }

   public boolean canEnchantItem(ItemStack itemStack, Enchantment enchantment) {
      if (!OnyxShop.getInstance().allowUnsafeEnchants && XMaterial.matchXMaterial(itemStack) != XMaterial.ENCHANTED_BOOK) {
         if (enchantment.canEnchantItem(itemStack)) {
            for (Enchantment enchantment2 : itemStack.getEnchantments().keySet()) {
               if (enchantment.conflictsWith(enchantment2)) {
                  SendMessage.errorMessage(
                     "Could not add enchantment '"
                        + enchantment.getKey().getKey()
                        + "' to this shop item because it conflicts with enchantment '"
                        + enchantment2.getKey().getKey()
                        + "'"
                  );
                  return false;
               }
            }

            return true;
         } else {
            SendMessage.errorMessage("Could not apply enchantment '" + enchantment.getKey().getKey() + "' to this shop item material type");
            return false;
         }
      } else {
         return true;
      }
   }

   public ItemStack addEnchantment(ItemStack itemStack, Enchantment enchantment, int value, @Nullable DisplayItem displayItem) {
      if (itemStack.getType().equals(XMaterial.ENCHANTED_BOOK.parseMaterial())) {
         EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta)itemStack.getItemMeta();
         enchantmentStorageMeta.addStoredEnchant(enchantment, value, true);
         itemStack.setItemMeta(enchantmentStorageMeta);
      } else {
         itemStack.addUnsafeEnchantment(enchantment, value);
      }

      return itemStack;
   }

   public ItemStack getBook(Enchantment enchantment, int value) {
      ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta enchantmentStorageMeta = (EnchantmentStorageMeta)itemStack.getItemMeta();
      enchantmentStorageMeta.addStoredEnchant(enchantment, value, true);
      itemStack.setItemMeta(enchantmentStorageMeta);
      return itemStack;
   }
}
