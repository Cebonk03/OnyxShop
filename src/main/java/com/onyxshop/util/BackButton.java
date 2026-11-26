package com.onyxshop.util;

import com.onyxshop.files.ConfigManager;

public final class BackButton {
   public static boolean DIRECT_CONSOLE;
   public static boolean DIRECT;
   public static boolean SUB_SECTIONS;
   public static boolean SHOP_SEARCH;

   public BackButton() {
   }

   public static void init() {
      DIRECT = ConfigManager.getConfig().getBoolean("disable-back.direct-shop-command");
      DIRECT_CONSOLE = ConfigManager.getConfig().getBoolean("disable-back.direct-shop-command-console");
      SUB_SECTIONS = ConfigManager.getConfig().getBoolean("disable-back.sub-sections");
      SHOP_SEARCH = ConfigManager.getConfig().getBoolean("disable-back.shop-search", true);
   }
}
