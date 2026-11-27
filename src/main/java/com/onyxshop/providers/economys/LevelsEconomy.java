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
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.levels");
      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(pathIgnoreCase + ".friendly", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-levels-currency-name-plural%", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
      SendMessage.infoMessage("Successfully enabled xp levels economy");
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return ((Player)offlinePlayer).getLevel();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      ((Player)offlinePlayer).giveExpLevels((int)Math.round(balance));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      ((Player)offlinePlayer).setLevel((int)Math.round(this.getBalance(offlinePlayer) - balance));
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
      String text2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(text + ".singular", Lang.LEVELS_CURRENCY_NAME_SINGULAR.get().getLegacy())
            .replace("%translations-levels-currency-name-singular%", Lang.LEVELS_CURRENCY_NAME_SINGULAR.get().getLegacy())
      );
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
         } else {
            this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
         }
      } else {
         this.singular = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(text + ".plural", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-levels-currency-name-plural%", Lang.LEVELS_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      if (!text2.isEmpty()) {
         if (this.isSymbol(text2)) {
            this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
         } else {
            this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
         }
      } else {
         this.plural = Lang.CURRENCYSYMBOL.get().getLegacy();
      }
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
