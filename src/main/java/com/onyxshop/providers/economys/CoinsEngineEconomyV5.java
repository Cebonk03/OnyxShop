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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;
import su.nightexpress.excellenteconomy.api.currency.ExcellentCurrency;

public class CoinsEngineEconomyV5 implements EconomyProvider {
   private ExcellentEconomyAPI api;
   private String currency;
   private ExcellentCurrency cur;
   private String friendly;
   private String singular;
   private String plural;
   private boolean decimal;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public CoinsEngineEconomyV5(String var1) throws EconomyLoadException {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (!var1.getServer().getPluginManager().isPluginEnabled("CoinsEngine") && !var1.getServer().getPluginManager().isPluginEnabled("ExcellentEconomy")) {
         throw new EconomyLoadException("ExcellentEconomy not enabled yet");
      } else {
         RegisteredServiceProvider var2 = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
         if (var2 != null) {
            this.api = (ExcellentEconomyAPI)var2.getProvider();
         }

         this.registerCurrency();
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String var1;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = this.api.getCurrency(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a ExcellentEconomy currency named as '" + this.currency + "'");
         }

         var1 = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from ExcellentEconomy");
      } else {
         this.cur = (ExcellentCurrency)this.api.getCurrencies().stream().findFirst().orElse(null);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from ExcellentEconomy");
         }

         var1 = this.cur.getName();
         SendMessage.infoMessage("Successfully hooked into ExcellentEconomy and using default currency: " + this.cur.getName());
      }

      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.excellent-economy:" + var1);
      if (!ConfigManager.getConfig().contains(var2)) {
         var2 = ConfigManager.getPathIgnoreCase("currency-formats.excellent-economy");
      }

      this.formatSingular(var2);
      this.formatPlural(var2);
      this.formatFriendly(var2);
      this.decimal = this.cur.isDecimal();
      this.priceFormat = PriceFormatter.getFormatter(var2);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return this.api.getBalance((Player)var1, this.cur);
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      this.api.deposit((Player)var1, this.cur, var2);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      this.api.withdraw((Player)var1, this.cur, var2);
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.EXCELLENT_ECONOMY, this.currency);
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
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", this.cur.getSymbol()));
      String var3 = ChatUtil.formatColors(
         this.cur.getFormat().replace("%amount%", !var2.contains("%price%") ? "%price%" : "").replace("%currency_symbol%", var2)
      );
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.singular = var3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy() : var3;
         } else {
            this.singular = var3.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy() : var3;
         }
      } else {
         this.singular = var3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : var3;
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", this.cur.getSymbol()));
      String var3 = ChatUtil.formatColors(
         this.cur.getFormat().replace("%amount%", !var2.contains("%price%") ? "%price%" : "").replace("%currency_symbol%", var2)
      );
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.plural = var3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy() : var3;
         } else {
            this.plural = var3.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy() : var3;
         }
      } else {
         this.plural = var3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : var3;
      }
   }

   private void formatFriendly(String var1) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", this.cur.getName()));
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
