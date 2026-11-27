package com.onyxshop.providers.economys;

import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.user.VotingPluginUser;
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

public class VotingPluginEconomy implements EconomyProvider {
   private String friendly;
   private String singular;
   private String plural;
   private boolean wait = false;
   private final boolean decimal = false;
   private PriceFormatter.Format priceFormat;

   public VotingPluginEconomy() {
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().getPlugin("VotingPlugin") != null) {
         SendMessage.infoMessage("Successfully hooked into VotingPlugin");
         String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.voting-plugin");
         this.formatSingular(pathIgnoreCase);
         this.formatPlural(pathIgnoreCase);
         this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(pathIgnoreCase + ".friendly", "Points"));
         this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
      } else {
         throw new EconomyLoadException("VotingPlugin is not installed");
      }
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      VotingPluginUser votingPluginUser = VotingPluginMain.plugin.getUser(offlinePlayer.getUniqueId());
      return votingPluginUser.getPoints();
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      VotingPluginUser votingPluginUser = VotingPluginMain.plugin.getUser(offlinePlayer.getUniqueId());
      votingPluginUser.setPoints((int)this.getBalance(offlinePlayer) + (int)Math.round(balance));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      VotingPluginUser votingPluginUser = VotingPluginMain.plugin.getUser(offlinePlayer.getUniqueId());
      if (votingPluginUser == null) {
         return false;
      }

      votingPluginUser.removePoints((int)Math.round(balance));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.VOTING_PLUGIN);
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
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".singular", "Point"));
      if (this.isSymbol(text2)) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private void formatPlural(String text) {
      String text2 = ChatUtil.formatColors(ConfigManager.getConfig().getString(text + ".plural", "Points"));
      if (this.isSymbol(text2)) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text2).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text2).getLegacy();
      }
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
