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
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.exp");
      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(pathIgnoreCase + ".friendly", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-exp-currency-name-plural%", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
      );
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
      SendMessage.infoMessage("Successfully enabled EXP points economy");
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.getTotalExperience((Player)offlinePlayer);
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.setTotalExperience((Player)offlinePlayer, this.getTotalExperience((Player)offlinePlayer) + (int)Math.round(balance));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      this.setTotalExperience((Player)offlinePlayer, this.getTotalExperience((Player)offlinePlayer) - (int)Math.round(balance));
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
            .getString(text + ".singular", Lang.XP_CURRENCY_NAME_SINGULAR.get().getLegacy())
            .replace("%translations-exp-currency-name-singular%", Lang.XP_CURRENCY_NAME_SINGULAR.get().getLegacy())
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
            .getString(text + ".plural", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
            .replace("%translations-exp-currency-name-plural%", Lang.XP_CURRENCY_NAME_PLURAL.get().getLegacy())
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

   private void setTotalExperience(Player player, int value) {
      player.setExp(0.0F);
      player.setLevel(0);
      player.setTotalExperience(0);
      int value2 = value;

      while (value2 > 0) {
         int expAtLevel = this.getExpAtLevel(player);
         value2 -= expAtLevel;
         if (value2 >= 0) {
            player.giveExp(expAtLevel);
         } else {
            value2 += expAtLevel;
            player.giveExp(value2);
            value2 = 0;
         }
      }
   }

   private int getExpAtLevel(Player player) {
      return this.getExpAtLevel(player.getLevel());
   }

   private int getExpAtLevel(int value) {
      if (value <= 15) {
         return 2 * value + 7;
      } else {
         return value >= 16 && value <= 30 ? 5 * value - 38 : 9 * value - 158;
      }
   }

   private int getExpToLevel(int value) {
      int value2 = 0;

      int value3;
      for (value3 = 0; value2 < value; value2++) {
         value3 += this.getExpAtLevel(value2);
      }

      if (value3 < 0) {
         value3 = Integer.MAX_VALUE;
      }

      return value3;
   }

   private int getTotalExperience(Player player) {
      int value = Math.round(this.getExpAtLevel(player) * player.getExp());
      int level = player.getLevel();

      while (level > 0) {
         value += this.getExpAtLevel(--level);
      }

      if (value < 0) {
         value = Integer.MAX_VALUE;
      }

      return value;
   }

   private int getExpUntilNextLevel(Player player) {
      int value = Math.round(this.getExpAtLevel(player) * player.getExp());
      int level = player.getLevel();
      return this.getExpAtLevel(level) - value;
   }
}
