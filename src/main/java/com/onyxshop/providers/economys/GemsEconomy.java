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

   public GemsEconomy(String text) {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().isPluginEnabled("GemsEconomy")) {
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
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = this.gems.getCurrency(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a GemsEconomy currency named as '" + this.currency + "'");
         }

         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from GemsEconomy");
      } else {
         this.cur = this.gems.plugin.getCurrencyManager().getDefaultCurrency();
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from GemsEconomy");
         }

         text = this.cur.getSingular();
         SendMessage.infoMessage("Successfully hooked into GemsEconomy and using default currency: " + text + "(" + this.cur.getPlural() + ")");
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.gems-economy:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.gems-economy");
      }

      this.decimal = this.cur.isDecimalSupported();
      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.gems.getBalance(offlinePlayer.getUniqueId(), this.cur);
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.gems.deposit(offlinePlayer.getUniqueId(), !this.decimal ? Math.round(balance) : balance, this.cur);
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      this.gems.withdraw(offlinePlayer.getUniqueId(), !this.decimal ? Math.round(balance) : balance, this.cur);
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
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", this.cur.getSingular()));
      if (this.isSymbol(text2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.cur.getPlural()));
      if (this.isSymbol(text2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", this.cur.getPlural()));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
