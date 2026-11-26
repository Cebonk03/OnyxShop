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

   public EcoBitsEconomy(String var1) {
      this.currency = var1;
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      if (var1.getServer().getPluginManager().getPlugin("EcoBits") == null) {
         throw new EconomyLoadException("EcoBits is not installed");
      } else {
         if (Currencies.values().isEmpty()) {
            this.wait = true;
            var1.runTaskLater(this::init, 20L);
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
         } catch (EconomyLoadException var2) {
            SendMessage.warnMessage(var2.getMessage());
         }

         if (this.wait) {
            this.wait = false;
            OnyxShop.getInstance().getEcoHandler().mayContinue();
         }
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String var1;
      if (this.currency != null && !this.currency.isEmpty()) {
         this.cur = Currencies.getByID(this.currency);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to register a EcoBits currency named as '" + this.currency + "'");
         }

         var1 = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.cur.getName() + "' from EcoBits");
      } else {
         this.cur = (Currency)Currencies.values().stream().findFirst().orElse(null);
         if (this.cur == null) {
            throw new EconomyLoadException("Failed to find default currency from EcoBits");
         }

         var1 = this.cur.getId();
         SendMessage.infoMessage("Successfully hooked into EcoBits and using default currency: " + var1 + "(" + this.cur.getName() + ")");
      }

      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.ecobits:" + var1);
      if (!ConfigManager.getConfig().contains(var2)) {
         var2 = ConfigManager.getPathIgnoreCase("currency-formats.ecobits");
      }

      this.formatSingular(var2);
      this.formatPlural(var2);
      this.formatFriendly(var2);
      this.decimal = this.cur.isDecimal();
      this.priceFormat = PriceFormatter.getFormatter(var2);
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return CurrencyUtils.getBalance(var1, this.cur).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      CurrencyUtils.setBalance(var1, this.cur, CurrencyUtils.getBalance(var1, this.cur).add(new BigDecimal(var2)));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      CurrencyUtils.setBalance(var1, this.cur, CurrencyUtils.getBalance(var1, this.cur).subtract(new BigDecimal(var2)));
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
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".singular", this.cur.getName()));
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
         } else {
            this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
         }
      } else {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".plural", this.cur.getName()));
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
         } else {
            this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
         }
      } else {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", Lang.CURRENCYSYMBOL.get()).getLegacy();
      }
   }

   private void formatFriendly(String var1) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(var1 + ".friendly", this.cur.getName()));
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
