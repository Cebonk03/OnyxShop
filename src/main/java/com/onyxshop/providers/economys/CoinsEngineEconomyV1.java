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
import org.bukkit.OfflinePlayer;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;
import su.nightexpress.coinsengine.data.impl.CoinsUser;

public class CoinsEngineEconomyV1 implements EconomyProvider {
   private String currency;
   private Currency cur;
   private String friendly;
   private String singular;
   private String plural;
   private boolean decimal;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public CoinsEngineEconomyV1(String text) throws EconomyLoadException {
      if (!text.isEmpty()) {
         this.currency = text;
      }
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (!plugin.getServer().getPluginManager().isPluginEnabled("CoinsEngine")) {
         throw new EconomyLoadException("Could not find CoinsEngine");
      } else {
         this.registerCurrency();
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = CoinsEngineAPI.getCurrencyManager().getCurrency(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a CoinsEngine currency named as '" + this.currency + "'");
         }

         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from CoinsEngine");
      } else {
         this.cur = (Currency)CoinsEngineAPI.getCurrencyManager().getCurrencies().stream().findFirst().orElse(null);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from CoinsEngine");
         }

         text = this.cur.getName();
         SendMessage.infoMessage("Successfully hooked into CoinsEngine and using default currency: " + this.cur.getName());
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
      CoinsUser coinsUser = (CoinsUser)CoinsEngineAPI.getUserManager().getUserData(offlinePlayer.getUniqueId());
      return coinsUser.getCurrencyData(this.cur).getBalance();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      CoinsEngineAPI.getUserManager().getUserDataAsync(offlinePlayer.getUniqueId()).thenAccept(value -> {
         if (value == null) {
            SendMessage.errorMessage("Failed to deposit balance: Cannot find player " + offlinePlayer.getName() + " in CoinsEngine");
         } else {
            value.getCurrencyData(this.cur).addBalance(balance);
            value.saveData(CoinsEngineAPI.PLUGIN);
         }
      });
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      CoinsEngineAPI.getUserManager().getUserDataAsync(offlinePlayer.getUniqueId()).thenAccept(value -> {
         if (value == null) {
            SendMessage.errorMessage("Failed to withdraw balance: Cannot find player " + offlinePlayer.getName() + " in CoinsEngine");
         } else {
            value.getCurrencyData(this.cur).removeBalance(balance);
            value.saveData(CoinsEngineAPI.PLUGIN);
         }
      });
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
      String price = ChatUtil.formatColors(this.cur.getFormat().replace("%amount%", "%price%").replace("%currency_symbol%", text2));
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.singular = price.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy() : price;
         } else {
            this.singular = price.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy() : price;
         }
      } else {
         this.singular = price.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : price;
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.cur.getSymbol()));
      String price = ChatUtil.formatColors(this.cur.getFormat().replace("%amount%", "%price%").replace("%currency_symbol%", text2));
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.plural = price.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy() : price;
         } else {
            this.plural = price.isEmpty() ? Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy() : price;
         }
      } else {
         this.plural = price.isEmpty() ? Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy() : price;
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", this.cur.getName()));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
