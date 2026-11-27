package com.onyxshop.methodes;

import java.nio.file.Paths;
import com.onyxshop.OnyxShop;
import com.onyxshop.database.Database;
import com.onyxshop.database.types.MySQL;
import com.onyxshop.database.types.PostgreSQL;
import com.onyxshop.database.types.SQLite;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.objects.shops.ShopType;
import com.onyxshop.providers.DisplayCacheProvider;
import com.onyxshop.providers.DynamicPricingProvider;
import com.onyxshop.providers.LimitedSellProvider;
import com.onyxshop.providers.LimitedStockProvider;
import com.onyxshop.util.ConfigUtil;
import org.bukkit.entity.Player;

public class ProviderManager {
   private final OnyxShop plugin;
   private Database db;
   private LimitedStockProvider limitedStockProvider;
   private LimitedSellProvider limitedSellProvider;
   private DynamicPricingProvider dynamicPricingProvider;
   private DisplayCacheProvider displayCacheProvider;

   public ProviderManager(OnyxShop plugin) {
      this.plugin = plugin;
   }

   public Database getDatabase() {
      return this.db;
   }

   public LimitedStockProvider getLimitedStockProvider() {
      return this.limitedStockProvider;
   }

   public LimitedSellProvider getLimitedSellProvider() {
      return this.limitedSellProvider;
   }

   public DynamicPricingProvider getDynamicPricingProvider() {
      return this.dynamicPricingProvider;
   }

   public DisplayCacheProvider getDisplayCacheProvider() {
      return this.displayCacheProvider;
   }

   public void reloadProviders() {
      boolean flag = this.usesLimitedStock();
      boolean flag2 = this.usesLimitedSell();
      boolean enabled = ConfigManager.getConfig().getBoolean("dynamic-pricing.enabled");
      if (this.db == null) {
         if (this.setupDatabase()) {
            SendMessage.infoMessage(Lang.DATABASE_CONNECTED.get().replace("%database-type%", this.db.getClass().getSimpleName()));
            if (flag) {
               this.limitedStockProvider = new LimitedStockProvider(this.plugin, this.db);
            }

            if (flag2) {
               this.limitedSellProvider = new LimitedSellProvider(this.plugin, this.db);
            }

            if (enabled) {
               this.dynamicPricingProvider = new DynamicPricingProvider(this.plugin, this.db);
            }
         } else {
            if (flag) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_LIMITEDSTOCK.get());
            }

            if (flag2) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_LIMITEDSELLSTOCK.get());
            }

            if (enabled) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_DYNAMICPRICING.get());
            }
         }
      } else {
         if (flag) {
            if (this.limitedStockProvider == null) {
               this.limitedStockProvider = new LimitedStockProvider(this.plugin, this.db);
            } else {
               this.limitedStockProvider.reload();
            }
         } else if (this.limitedStockProvider != null) {
            this.limitedStockProvider.stop();
            this.limitedStockProvider = null;
         }

         if (flag2) {
            if (this.limitedSellProvider == null) {
               this.limitedSellProvider = new LimitedSellProvider(this.plugin, this.db);
            } else {
               this.limitedSellProvider.reload();
            }
         } else if (this.limitedSellProvider != null) {
            this.limitedSellProvider.stop();
            this.limitedSellProvider = null;
         }

         if (enabled) {
            if (this.dynamicPricingProvider == null) {
               this.dynamicPricingProvider = new DynamicPricingProvider(this.plugin, this.db);
            } else {
               this.dynamicPricingProvider.reload();
            }
         } else {
            if (this.dynamicPricingProvider != null) {
               this.dynamicPricingProvider.stop();
               this.dynamicPricingProvider = null;
            }

            this.plugin.dynamicPricing = false;
         }
      }

      if (this.db instanceof MySQL || this.db instanceof PostgreSQL) {
         String text = ConfigManager.getConfig().getString(this.db instanceof PostgreSQL ? "postgresql" : "mysql.display-cache", "AUTO");
         if (text.equals("-1") || text.equals("DISABLED")) {
            this.plugin.displayCache = false;
            if (this.displayCacheProvider != null) {
               this.displayCacheProvider.stop();
               this.displayCacheProvider = null;
            }
         } else if (this.displayCacheProvider == null) {
            this.displayCacheProvider = new DisplayCacheProvider(this.plugin);
         } else {
            this.displayCacheProvider.reload();
         }
      }
   }

   public void stopProviders() {
      if (this.limitedStockProvider != null) {
         this.limitedStockProvider.stop();
      }

      if (this.limitedSellProvider != null) {
         this.limitedSellProvider.stop();
      }

      if (this.dynamicPricingProvider != null) {
         this.dynamicPricingProvider.stop();
      }

      if (this.displayCacheProvider != null) {
         this.displayCacheProvider.stop();
      }

      if (this.db != null) {
         this.db.closeConnection();
         this.db = null;
      }
   }

   public void resetDynamicPricing(Object value) {
      if (this.db instanceof SQLite) {
         ConfigUtil.createBackup(Paths.get(OnyxShop.getInstance().getDataFolder() + "/database.db"));
      }

      SendMessage.sendMessage(value, Lang.RESETTING_DYNAMICPRICING.get());
      if (this.db.resetDynamicPricing()) {
         if (this.dynamicPricingProvider != null) {
            this.dynamicPricingProvider.stop();
         }

         this.dynamicPricingProvider = new DynamicPricingProvider(this.plugin, this.db);
         SendMessage.sendMessage(value, Lang.DYNAMICPRICING_RESET.get());
      } else if (value instanceof Player) {
         SendMessage.errorMessage(value, "A error occurred while trying to reset the DynamicPricing from the database, see console for error logs");
      }
   }

   private boolean setupDatabase() {
      String text = ConfigManager.getConfig().getString("storage-type", "");
      Object value;
      switch (text) {
         case "MySQL":
            value = new MySQL();
            break;
         case "PostgreSQL":
            value = new PostgreSQL();
            break;
         case "SQLite":
            value = new SQLite();
            break;
         default:
            SendMessage.warnMessage(Lang.INVALID_STORAGE_TYPE.get().replace("%type%", text));
            value = new SQLite();
      }

      this.db = (Database)value;
      return ((Database)value).connect();
   }

   private boolean usesLimitedStock() {
      for (String itemId : ConfigManager.getShops()) {
         for (String text : this.plugin.getConfigManager().getItemsRaw(itemId)) {
            String shopId = (this.plugin.getShopType(itemId) == ShopType.CATEGORY ? "pages." : "") + text;
            if (ConfigManager.getShop(itemId).contains(shopId + ".stock-limit-player") || ConfigManager.getShop(itemId).contains(shopId + ".stock-limit-global")) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean usesLimitedSell() {
      for (String text : ConfigManager.getShops()) {
         for (String text2 : this.plugin.getConfigManager().getItemsRaw(text)) {
            String shopId = (this.plugin.getShopType(text) == ShopType.CATEGORY ? "pages." : "") + text2;
            if (ConfigManager.getShop(text).contains(shopId + ".sell-limit-player") || ConfigManager.getShop(text).contains(shopId + ".sell-limit-global")) {
               return true;
            }
         }
      }

      return false;
   }
}
