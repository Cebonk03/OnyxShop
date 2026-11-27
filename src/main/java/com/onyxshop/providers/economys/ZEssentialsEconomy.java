package com.onyxshop.providers.economys;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.economy.Economy;
import fr.maxlego08.essentials.api.economy.EconomyManager;
import java.math.BigDecimal;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
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

public class ZEssentialsEconomy implements EconomyProvider {
   private EconomyManager manager;
   private Economy cur;
   private String currency;
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = true;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public ZEssentialsEconomy(String text) {
      this.currency = text;
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().isPluginEnabled("zEssentials")) {
         EssentialsPlugin essentialsPlugin = (EssentialsPlugin)Bukkit.getServer().getPluginManager().getPlugin("zEssentials");
         if (essentialsPlugin != null) {
            this.registerCurrency(essentialsPlugin.getEconomyManager());
         } else {
            throw new EconomyLoadException("Failed to hook into zEssentials");
         }
      } else {
         throw new EconomyLoadException("Could not find zEssentials");
      }
   }

   private void registerCurrency(EconomyManager economyManager) throws EconomyLoadException {
      String text;
      if (this.currency != null && !this.currency.isEmpty()) {
         Optional optional = economyManager.getEconomy(this.currency);
         if (!optional.isPresent()) {
            throw new EconomyLoadException("Failed to register a zEssentials currency named as '" + this.currency + "'");
         }

         this.cur = (Economy)optional.get();
         text = this.currency;
         SendMessage.infoMessage("Successfully registered currency '" + this.currency + "' from zEssentials");
      } else {
         try {
            this.cur = economyManager.getDefaultEconomy();
            text = this.cur.getName();
            SendMessage.infoMessage("Successfully hooked into zEssentials and using default currency: " + this.cur.getName() + "(" + this.cur.getSymbol() + ")");
         } catch (IndexOutOfBoundsException exception) {
            throw new EconomyLoadException("Failed to find a currency in zEssentials");
         }
      }

      String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.zEssentials:" + text);
      if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
         pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.zEssentials");
      }

      this.manager = economyManager;
      this.formatSingular(pathIgnoreCase);
      this.formatPlural(pathIgnoreCase);
      this.formatFriendly(pathIgnoreCase);
      this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.manager.getBalance(offlinePlayer, this.cur).doubleValue();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.manager.deposit(offlinePlayer.getUniqueId(), this.cur, BigDecimal.valueOf(balance));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      this.manager.withdraw(offlinePlayer.getUniqueId(), this.cur, BigDecimal.valueOf(balance));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ZESSENTIALS, this.currency);
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
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", this.cur.getSymbol()));
      if (this.isSymbol(text2)) {
         this.singular = this.cur.getFormat().replace("%symbol%", text2);
      } else {
         this.singular = this.cur.getFormat().replace("%currency-format%", text2);
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", this.cur.getSymbol()));
      if (this.isSymbol(text2)) {
         this.plural = this.cur.getFormat().replace("%symbol%", text2);
      } else {
         this.plural = this.cur.getFormat().replace("%currency-format%", text2);
      }
   }

   private void formatFriendly(String text) {
      this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".friendly", this.cur.getSymbol()));
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
