package com.onyxshop.providers.economys;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.exceptions.EconomyLoadException;
import me.xanium.gemseconomy.api.GemsEconomyAPI;
import me.xanium.gemseconomy.currency.Currency;
import org.bukkit.OfflinePlayer;

public class GemsEconomy implements EconomyProvider {
   private GemsEconomyAPI gems;
   private String currency;
   private boolean decimal;
   private Currency cur;
   private String friendly;
   private String singular;
   private String plural;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public GemsEconomy(String var1) {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().isPluginEnabled("GemsEconomy")) {
         this.gems = new GemsEconomyAPI();
         if (this.gems != null) {
            this.registerCurrency();
         } else {
            throw new EconomyLoadException("Failed to hook into GemsEconomy");
         }
      } else {
         throw new EconomyLoadException("Could not find GemsEconomy");
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String var1;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = this.gems.getCurrency(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a GemsEconomy currency named as '" + this.currency + "'");
         }

         var1 = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from GemsEconomy");
      } else {
         this.cur = this.gems.plugin.getCurrencyManager().getDefaultCurrency();
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from GemsEconomy");
         }

         var1 = this.cur.getSingular();
         SendMessage.infoMessage("Successfully hooked into GemsEconomy and using default currency: " + var1 + "(" + this.cur.getPlural() + ")");
      }

      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.gems-economy:" + var1);
      if (!ConfigManager.getConfig().contains(var2)) {
         var2 = ConfigManager.getPathIgnoreCase("currency-formats.gems-economy");
      }

      this.decimal = this.cur.isDecimalSupported();
      this.formatSingular(var2);
      this.formatPlural(var2);
      this.formatFriendly(var2);
      this.priceFormat = PriceFormatter.getFormatter(var2);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return this.gems.getBalance(var1.getUniqueId(), this.cur);
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      this.gems.deposit(var1.getUniqueId(), !this.decimal ? Math.round(var2) : var2, this.cur);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      this.gems.withdraw(var1.getUniqueId(), !this.decimal ? Math.round(var2) : var2, this.cur);
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.GEMS_ECONOMY, this.currency);
   }

   @Override
   public String getPlural() {
      return this.plural;
   }

   @Override
   public String getSingular() {
      return this.singular;
   }

   @Override
   public String getFriendly() {
      return this.friendly;
   }

   @Override
   public boolean isDecimal() {
      return this.decimal;
   }

   @Override
   public boolean doWait() {
      return this.wait;
   }

   @Override
   public double round(double var1) {
      return this.priceFormat.round(var1);
   }

   @Override
   public String formatPrice(double var1) {
      return this.priceFormat.format(this, var1);
   }

   @Override
   public void formatPlaceholders() {
      this.singular = PlaceholderAPIHook.translate(null, this.singular);
      this.plural = PlaceholderAPIHook.translate(null, this.plural);
      this.friendly = PlaceholderAPIHook.translate(null, this.friendly);
   }

   private void formatSingular(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", this.cur.getSingular()));
      if (this.isSymbol(var2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", this.cur.getPlural()));
      if (this.isSymbol(var2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
      }
   }

   private void formatFriendly(String var1) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", this.cur.getPlural()));
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
