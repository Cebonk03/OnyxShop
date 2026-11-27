package com.onyxshop.providers.economys;

import java.util.Optional;
import me.TechsCode.UltraEconomy.UltraEconomyAPI;
import me.TechsCode.UltraEconomy.objects.Account;
import me.TechsCode.UltraEconomy.objects.Currency;
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

public class UltraEconomy implements EconomyProvider {
   private UltraEconomyAPI ue;
   private Currency cur;
   private String currency;
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = true;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public UltraEconomy(String text) {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().isPluginEnabled("UltraEconomy")) {
         this.ue = me.TechsCode.UltraEconomy.UltraEconomy.getAPI();
         if (this.ue != null) {
            this.registerCurrency();
         } else {
            throw new EconomyLoadException("Failed to hook into UltraEconomy");
         }
      } else {
         throw new EconomyLoadException("Could not find UltraEconomy");
      }
   }

   private void registerCurrency() throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         Optional optional = this.ue.getCurrencies().name(this.currency);
         if (!optional.isPresent()) {
            throw new EconomyLoadException("Failed to register a UltraEconomy currency named as '" + this.currency + "'");
         }

         this.cur = (Currency)optional.get();
         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from UltraEconomy");
      } else {
         try {
            this.cur = (Currency)this.ue.getCurrencies().get(0);
            text = this.cur.getName();
            SendMessage.infoMessage("Successfully hooked into UltraEconomy and using default currency: " + this.cur.getKey() + "(" + this.cur.getName() + ")");
         } catch (IndexOutOfBoundsException exception) {
            throw new EconomyLoadException("Failed to find a currency in UltraEconomy");
         }
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.ultra-economy:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.ultra-economy");
      }

      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      Optional optional = this.ue.getAccounts().uuid(offlinePlayer.getUniqueId());
      if (optional.isPresent()) {
         return ((Account)optional.get()).getBalance(this.cur).getOnHand();
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + offlinePlayer.getName() + "'");
         return 0.0;
      }
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      Optional optional = this.ue.getAccounts().uuid(offlinePlayer.getUniqueId());
      if (optional.isPresent()) {
         ((Account)optional.get()).getBalance(this.cur).addHand((float)balance);
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + offlinePlayer.getName() + "'");
      }
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      Optional optional = this.ue.getAccounts().uuid(offlinePlayer.getUniqueId());
      if (optional.isPresent()) {
         ((Account)optional.get()).getBalance(this.cur).removeHand((float)balance);
         return true;
      } else {
         SendMessage.warnMessage("UltraEconomy Failed to retrieve the account from player '" + offlinePlayer.getName() + "'");
         return false;
      }
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ULTRA_ECONOMY, this.currency);
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
      return true;
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
            .getString(text + ".singular", this.cur.getFormat().getSingularFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
      if (this.isSymbol(text2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(text + ".plural", this.cur.getFormat().getPluralFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
      if (this.isSymbol(text2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(
         ConfigManager.getConfig()
            .getString(text + ".friendly", this.cur.getFormat().getPluralFormat().replace("{Amount} ", "").replace(" {Amount}", "").replace("{Amount}", ""))
      );
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
