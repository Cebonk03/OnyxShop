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

   public CoinsEngineEconomyV5(String text) throws EconomyLoadException {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (!plugin.getServer().getPluginManager().isPluginEnabled("CoinsEngine") && !plugin.getServer().getPluginManager().isPluginEnabled("ExcellentEconomy")) {
         throw new EconomyLoadException("ExcellentEconomy not enabled yet");
      } else {
         RegisteredServiceProvider registeredServiceProvider = Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);
         if (registeredServiceProvider != null) {
            this.api = (ExcellentEconomyAPI)registeredServiceProvider.getProvider();
         }

         this.registerCurrency();
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = this.api.getCurrency(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a ExcellentEconomy currency named as '" + this.currency + "'");
         }

         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from ExcellentEconomy");
      } else {
         this.cur = (ExcellentCurrency)this.api.getCurrencies().stream().findFirst().orElse(null);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from ExcellentEconomy");
         }

         text = this.cur.getName();
         SendMessage.infoMessage("Successfully hooked into ExcellentEconomy and using default currency: " + this.cur.getName());
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.excellent-economy:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.excellent-economy");
      }

      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.decimal = this.cur.isDecimal();
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.api.getBalance((Player)offlinePlayer, this.cur);
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.api.deposit((Player)offlinePlayer, this.cur, balance);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      this.api.withdraw((Player)offlinePlayer, this.cur, balance);
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
   public double round(double amount) {
      return this.priceFormat.round(amount);
   }

   @Override
   public String formatPrice(double price) {
      return this.priceFormat.format(this, price);
   }

   @Override
   public void formatPlaceholders() {
      this.singular = PlaceholderAPIHook.translate(null, this.singular);
      this.plural = PlaceholderAPIHook.translate(null, this.plural);
      this.friendly = PlaceholderAPIHook.translate(null, this.friendly);
   }

   private void formatSingular(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", this.cur.getSymbol()));
      String text3 = ChatUtil.formatColors(
         this.cur.getFormat().replace("%amount%", !text2.contains("%price%") ? "%price%" : "").replace("%currency_symbol%", text2)
      );
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.singular = text3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy() : text3;
         } else {
            this.singular = text3.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy() : text3;
         }
      } else {
         this.singular = text3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : text3;
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.cur.getSymbol()));
      String text3 = ChatUtil.formatColors(
         this.cur.getFormat().replace("%amount%", !text2.contains("%price%") ? "%price%" : "").replace("%currency_symbol%", text2)
      );
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.plural = text3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy() : text3;
         } else {
            this.plural = text3.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy() : text3;
         }
      } else {
         this.plural = text3.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : text3;
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", this.cur.getName()));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
