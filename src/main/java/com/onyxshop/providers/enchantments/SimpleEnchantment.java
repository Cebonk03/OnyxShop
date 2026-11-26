package com.onyxshop.providers.enchantments;

import com.onyxshop.objects.DisplayItem;
import com.onyxshop.providers.EnchantsProvider;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SimpleEnchantment {
   private final EnchantsProvider.Provider provider;
   private final Object enchant;
   private final String key;
   private final int lvl;

   public SimpleEnchantment(EnchantsProvider.Provider var1, Object var2, String var3, int var4) {
      this.provider = var1;
      this.enchant = var2;
      this.key = var3;
      this.lvl = var4;
   }

   public EnchantsProvider.Provider getProvider() {
      return this.provider;
   }

   public Object getEnchant() {
      return this.enchant;
   }

   public String getKey() {
      return this.key;
   }

   public int getLvl() {
      return this.lvl;
   }

   public boolean canEnchant(ItemStack var1) {
      return EnchantsProvider.canEnchant(var1, this);
   }

   public ItemStack getBook() {
      return EnchantsProvider.getBook(this);
   }

   public String getName() {
      return EnchantsProvider.getName(this);
   }

   public ItemStack enchantItem(ItemStack var1, @Nullable DisplayItem var2) {
      return EnchantsProvider.enchantItem(var1, this, var2);
   }
}
