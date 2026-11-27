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

   public SimpleEnchantment(EnchantsProvider.Provider provider, Object value, String text, int value2) {
      this.provider = provider;
      this.enchant = value;
      this.key = text;
      this.lvl = value2;
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

   public boolean canEnchant(ItemStack itemStack) {
      return EnchantsProvider.canEnchant(itemStack, this);
   }

   public ItemStack getBook() {
      return EnchantsProvider.getBook(this);
   }

   public String getName() {
      return EnchantsProvider.getName(this);
   }

   public ItemStack enchantItem(ItemStack itemStack, @Nullable DisplayItem displayItem) {
      return EnchantsProvider.enchantItem(itemStack, this, displayItem);
   }
}
