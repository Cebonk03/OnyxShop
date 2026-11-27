package com.onyxshop.providers.economys;

import com.willfp.ecobits.currencies.Currencies;
import com.willfp.ecobits.currencies.Currency;
import com.willfp.ecobits.currencies.CurrencyUtils;
import java.math.BigDecimal;
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

public class EcoBitsEconomy implements EconomyProvider {
   private String currency;
   private Currency cur;
   private String friendly;
   private String singular;
   private String plural;
   private boolean decimal;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public EcoBitsEconomy(String text) {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().getPlugin("EcoBits") == null) {
         throw new EconomyLoadException("EcoBits is not installed");
      } else {
         if (Currencies.values().isEmpty()) {
            this.wait = true;
            plugin.runTaskLater(this::init, 20L);
         } else {
            this.init();
         }
      }
   }

   private void init() {
      if (this.wait && Currencies.values().isEmpty()) {
         this.wait = false;
         SendMessage.warnMessage("EcoBits still didn't finish loading, aborting without EcoBits... Please make sure it is up-to date.");
         OnyxShop.getInstance().getEcoHandler().mayContinue();
      } else {
         try {
            this.registerCurrency();
         } catch (EconomyLoadException exception) {
            SendMessage.warnMessage(exception.getMessage());
         }

         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = Currencies.getByID(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a EcoBits currency named as '" + this.currency + "'");
         }

         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.cur.getName() + "' from EcoBits");
      } else {
         this.cur = (Currency)Currencies.values().stream().findFirst().orElse(null);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from EcoBits");
         }

         text = this.cur.getId();
         SendMessage.infoMessage("Successfully hooked into EcoBits and using default currency: " + text + "(" + this.cur.getName() + ")");
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.ecobits:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.ecobits");
      }

      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.decimal = this.cur.isDecimal();
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return CurrencyUtils.getBalance(offlinePlayer, this.cur).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      CurrencyUtils.setBalance(offlinePlayer, this.cur, CurrencyUtils.getBalance(offlinePlayer, this.cur).add(new BigDecimal(balance)));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      CurrencyUtils.setBalance(offlinePlayer, this.cur, CurrencyUtils.getBalance(offlinePlayer, this.cur).subtract(new BigDecimal(balance)));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ECOBITS, this.currency);
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
      String name = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", this.cur.getName()));
      if (!name.isEmpty()) {
         if (this.isSymbol(name)) {
            this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", name).getLegacy();
         } else {
            this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", name).getLegacy();
         }
      } else {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy();
      }
   }

   private void formatPlural(String text) {
      String name = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.cur.getName()));
      if (!name.isEmpty()) {
         if (this.isSymbol(name)) {
            this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", name).getLegacy();
         } else {
            this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", name).getLegacy();
         }
      } else {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy();
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", this.cur.getName()));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
