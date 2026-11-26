package com.onyxshop.providers.economys;

import com.onyxshop.OnyxShop;
import com.onyxshop.util.exceptions.EconomyLoadException;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class LevelsEconomy implements EconomyProvider {
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = false;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public LevelsEconomy() {
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.levels");
      this.formatSingular(var2);
      this.formatPlural(var2);
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var2 + ".friendly", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-levels-currency-name-plural%", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      this.priceFormat = PriceFormatter.getFormatter(var2);
      SendMessage.infoMessage("Successfully enabled xp levels economy");
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return ((Player)var1).getLevel();
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      ((Player)var1).giveExpLevels((int)Math.round(var2));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      ((Player)var1).setLevel((int)Math.round(this.getBalance(var1) - var2));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.LEVELS);
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
      return false;
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
      String var2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var1 + ".singular", Lang.LEVELS_CURRENCY_NAME_SINGULAR.get().getLegacy())
            .replace("%translations-levels-currency-name-singular%", Lang.LEVELS_CURRENCY_NAME_SINGULAR.get().getLegacy())
      );
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
         } else {
            this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
         }
      } else {
         this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private void formatPlural(String var1) {
      String var2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var1 + ".plural", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-levels-currency-name-plural%", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      if (!var2.isEmpty()) {
         if (this.isSymbol(var2)) {
            this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var2).getLegacy();
         } else {
            this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var2).getLegacy();
         }
      } else {
         this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
