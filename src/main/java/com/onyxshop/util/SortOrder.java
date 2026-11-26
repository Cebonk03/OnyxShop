package com.onyxshop.util;

import java.util.Locale;
import com.onyxshop.files.ConfigManager;

public enum SortOrder {
   SHOP_LOAD_ORDER,
   ASCENDING_SELL_PRICE,
   DESCENDING_SELL_PRICE;

   private SortOrder() {
   }

   public static SortOrder fromConfig() {
      String var0 = ConfigManager.getConfig().getString("sort-items-by", "DESCENDING_PRICE");

      try {
         return valueOf(var0.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var2) {
         return SHOP_LOAD_ORDER;
      }
   }
}
