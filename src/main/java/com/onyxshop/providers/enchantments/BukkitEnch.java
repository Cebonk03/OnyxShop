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

   public String getName(SimpleEnchantment var1) {
      return ((Enchantment)var1.getEnchant()).getName();
   }

   public boolean canEnchantItem(ItemStack var1, Enchantment var2) {
      if (!OnyxShop.getInstance().allowUnsafeEnchants && XMaterial.matchXMaterial(var1) != XMaterial.ENCHANTED_BOOK) {
         if (var2.canEnchantItem(var1)) {
            for (Enchantment var4 : var1.getEnchantments().keySet()) {
               if (var2.conflictsWith(var4)) {
                  SendMessage.errorMessage(
                     "Could not add enchantment '"
                        + var2.getKey().getKey()
                        + "' to this shop item because it conflicts with enchantment '"
                        + var4.getKey().getKey()
                        + "'"
                  );
                  return false;
               }
            }

            return true;
         } else {
            SendMessage.errorMessage("Could not apply enchantment '" + var2.getKey().getKey() + "' to this shop item material type");
            return false;
         }
      } else {
         return true;
      }
   }

   public ItemStack addEnchantment(ItemStack var1, Enchantment var2, int var3, @Nullable DisplayItem var4) {
      if (var1.getType().equals(XMaterial.ENCHANTED_BOOK.parseMaterial())) {
         EnchantmentStorageMeta var5 = (EnchantmentStorageMeta)var1.getItemMeta();
         var5.addStoredEnchant(var2, var3, true);
         var1.setItemMeta(var5);
      } else {
         var1.addUnsafeEnchantment(var2, var3);
      }

      return var1;
   }

   public ItemStack getBook(Enchantment var1, int var2) {
      ItemStack var3 = new ItemStack(Material.ENCHANTED_BOOK);
      EnchantmentStorageMeta var4 = (EnchantmentStorageMeta)var3.getItemMeta();
      var4.addStoredEnchant(var1, var2, true);
      var3.setItemMeta(var4);
      return var3;
   }
}
