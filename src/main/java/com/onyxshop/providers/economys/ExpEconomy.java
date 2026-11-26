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

public class ExpEconomy implements EconomyProvider {
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = false;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public ExpEconomy() {
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      String var2 = ConfigManager.getPathIgnoreCase("currency-formats.exp");
      this.formatSingular(var2);
      this.formatPlural(var2);
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(var2 + ".friendly", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-exp-currency-name-plural%", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      this.priceFormat = PriceFormatter.getFormatter(var2);
      SendMessage.infoMessage("Successfully enabled EXP points economy");
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      return this.getTotalExperience((Player)var1);
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      this.setTotalExperience((Player)var1, this.getTotalExperience((Player)var1) + (int)Math.round(var2));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      this.setTotalExperience((Player)var1, this.getTotalExperience((Player)var1) - (int)Math.round(var2));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.EXP);
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
            .getString(var1 + ".singular", Lang.XP_CURRENCY_NAME_SINGULAR.get().getLegacy())
            .replace("%translations-exp-currency-name-singular%", Lang.XP_CURRENCY_NAME_SINGULAR.get().getLegacy())
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
            .getString(var1 + ".plural", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-exp-currency-name-plural%", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
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

   private void setTotalExperience(Player var1, int var2) {
      var1.setExp(0.0F);
      var1.setLevel(0);
      var1.setTotalExperience(0);
      int var3 = var2;

      while (var3 > 0) {
         int var4 = this.getExpAtLevel(var1);
         var3 -= var4;
         if (var3 >= 0) {
            var1.giveExp(var4);
         } else {
            var3 += var4;
            var1.giveExp(var3);
            var3 = 0;
         }
      }
   }

   private int getExpAtLevel(Player var1) {
      return this.getExpAtLevel(var1.getLevel());
   }

   private int getExpAtLevel(int var1) {
      if (var1 <= 15) {
         return 2 * var1 + 7;
      } else {
         return var1 >= 16 && var1 <= 30 ? 5 * var1 - 38 : 9 * var1 - 158;
      }
   }

   private int getExpToLevel(int var1) {
      int var2 = 0;

      int var3;
      for (var3 = 0; var2 < var1; var2++) {
         var3 += this.getExpAtLevel(var2);
      }

      if (var3 < 0) {
         var3 = Integer.MAX_VALUE;
      }

      return var3;
   }

   private int getTotalExperience(Player var1) {
      int var2 = Math.round(this.getExpAtLevel(var1) * var1.getExp());
      int var3 = var1.getLevel();

      while (var3 > 0) {
         var2 += this.getExpAtLevel(--var3);
      }

      if (var2 < 0) {
         var2 = Integer.MAX_VALUE;
      }

      return var2;
   }

   private int getExpUntilNextLevel(Player var1) {
      int var2 = Math.round(this.getExpAtLevel(var1) * var1.getExp());
      int var3 = var1.getLevel();
      return this.getExpAtLevel(var3) - var2;
   }
}
