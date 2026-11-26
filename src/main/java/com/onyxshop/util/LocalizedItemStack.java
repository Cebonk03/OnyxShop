package com.onyxshop.util;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import com.onyxshop.files.Lang;
import com.onyxshop.providers.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LocalizedItemStack {
   private final ItemStack single;
   private final Map<Locale, ItemStack> variants;

   public LocalizedItemStack(ItemStack var1) {
      this.single = var1;
      this.variants = null;
   }

   public LocalizedItemStack(Map<Locale, ItemStack> var1) {
      this.single = null;
      this.variants = var1;
   }

   public ItemStack getItem(Player var1) {
      if (this.single != null) {
         return this.single;
      } else {
         ItemStack var2 = this.variants.get(UserManager.getUser(var1).getLocale());
         return var2 != null ? var2 : this.variants.get(Lang.getDefaultLocale());
      }
   }

   public ItemStack getItem() {
      return this.single != null ? this.single.clone() : this.variants.get(Lang.getDefaultLocale()).clone();
   }

   public Map<Locale, ItemStack> getVariants() {
      return this.single != null ? Collections.singletonMap(Lang.getDefaultLocale(), this.single) : this.variants;
   }
}
