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

   public LocalizedItemStack(ItemStack itemStack) {
      this.single = itemStack;
      this.variants = null;
   }

   public LocalizedItemStack(Map<Locale, ItemStack> map) {
      this.single = null;
      this.variants = map;
   }

   public ItemStack getItem(Player player) {
      if (this.single != null) {
         return this.single;
      } else {
         ItemStack itemStack = this.variants.get(UserManager.getUser(player).getLocale());
         return itemStack != null ? itemStack : this.variants.get(Lang.getDefaultLocale());
      }
   }

   public ItemStack getItem() {
      return this.single != null ? this.single.clone() : this.variants.get(Lang.getDefaultLocale()).clone();
   }

   public Map<Locale, ItemStack> getVariants() {
      return this.single != null ? Collections.singletonMap(Lang.getDefaultLocale(), this.single) : this.variants;
   }
}
