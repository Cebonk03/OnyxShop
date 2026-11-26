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

   public ProviderManager(OnyxShop var1) {
      this.plugin = var1;
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
      boolean var1 = this.usesLimitedStock();
      boolean var2 = this.usesLimitedSell();
      boolean var3 = ConfigManager.getConfig().getBoolean("dynamic-pricing.enabled");
      if (this.db == null) {
         if (this.setupDatabase()) {
            SendMessage.infoMessage(Lang.DATABASE_CONNECTED.get().replace("%database-type%", this.db.getClass().getSimpleName()));
            if (var1) {
               this.limitedStockProvider = new LimitedStockProvider(this.plugin, this.db);
            }

            if (var2) {
               this.limitedSellProvider = new LimitedSellProvider(this.plugin, this.db);
            }

            if (var3) {
               this.dynamicPricingProvider = new DynamicPricingProvider(this.plugin, this.db);
            }
         } else {
            if (var1) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_LIMITEDSTOCK.get());
            }

            if (var2) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_LIMITEDSELLSTOCK.get());
            }

            if (var3) {
               SendMessage.warnMessage(Lang.COULD_NOT_ENABLE_DYNAMICPRICING.get());
            }
         }
      } else {
         if (var1) {
            if (this.limitedStockProvider == null) {
               this.limitedStockProvider = new LimitedStockProvider(this.plugin, this.db);
            } else {
               this.limitedStockProvider.reload();
            }
         } else if (this.limitedStockProvider != null) {
            this.limitedStockProvider.stop();
            this.limitedStockProvider = null;
         }

         if (var2) {
            if (this.limitedSellProvider == null) {
               this.limitedSellProvider = new LimitedSellProvider(this.plugin, this.db);
            } else {
               this.limitedSellProvider.reload();
            }
         } else if (this.limitedSellProvider != null) {
            this.limitedSellProvider.stop();
            this.limitedSellProvider = null;
         }

         if (var3) {
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
         String var4 = ConfigManager.getConfig().getString(this.db instanceof PostgreSQL ? "postgresql" : "mysql.display-cache", "AUTO");
         if (var4.equals("-1") || var4.equals("DISABLED")) {
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

   public void resetDynamicPricing(Object var1) {
      if (this.db instanceof SQLite) {
         ConfigUtil.createBackup(Paths.get(OnyxShop.getInstance().getDataFolder() + "/database.db"));
      }

      SendMessage.sendMessage(var1, Lang.RESETTING_DYNAMICPRICING.get());
      if (this.db.resetDynamicPricing()) {
         if (this.dynamicPricingProvider != null) {
            this.dynamicPricingProvider.stop();
         }

         this.dynamicPricingProvider = new DynamicPricingProvider(this.plugin, this.db);
         SendMessage.sendMessage(var1, Lang.DYNAMICPRICING_RESET.get());
      } else if (var1 instanceof Player) {
         SendMessage.errorMessage(var1, "A error occurred while trying to reset the DynamicPricing from the database, see console for error logs");
      }
   }

   private boolean setupDatabase() {
      String var2 = ConfigManager.getConfig().getString("storage-type", "");
      Object var1;
      switch (var2) {
         case "MySQL":
            var1 = new MySQL();
            break;
         case "PostgreSQL":
            var1 = new PostgreSQL();
            break;
         case "SQLite":
            var1 = new SQLite();
            break;
         default:
            SendMessage.warnMessage(Lang.INVALID_STORAGE_TYPE.get().replace("%type%", var2));
            var1 = new SQLite();
      }

      this.db = (Database)var1;
      return ((Database)var1).connect();
   }

   private boolean usesLimitedStock() {
      for (String var2 : ConfigManager.getShops()) {
         for (String var4 : this.plugin.getConfigManager().getItemsRaw(var2)) {
            String var5 = (this.plugin.getShopType(var2) == ShopType.CATEGORY ? "pages." : "") + var4;
            if (ConfigManager.getShop(var2).contains(var5 + ".stock-limit-player") || ConfigManager.getShop(var2).contains(var5 + ".stock-limit-global")) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean usesLimitedSell() {
      for (String var2 : ConfigManager.getShops()) {
         for (String var4 : this.plugin.getConfigManager().getItemsRaw(var2)) {
            String var5 = (this.plugin.getShopType(var2) == ShopType.CATEGORY ? "pages." : "") + var4;
            if (ConfigManager.getShop(var2).contains(var5 + ".sell-limit-player") || ConfigManager.getShop(var2).contains(var5 + ".sell-limit-global")) {
               return true;
            }
         }
      }

      return false;
   }
}
