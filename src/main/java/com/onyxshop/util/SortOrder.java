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
      String text = ConfigManager.getConfig().getString("sort-items-by", "DESCENDING_PRICE");

      try {
         return valueOf(text.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException exception) {
         return SHOP_LOAD_ORDER;
      }
   }
}
