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
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.OfflinePlayer;

public class PlayerPointsEconomy implements EconomyProvider {
   private PlayerPointsAPI pp;
   private String friendly;
   private String singular;
   private String plural;
   private final boolean decimal = false;
   private boolean wait = false;
   private PriceFormatter.Format priceFormat;

   public PlayerPointsEconomy() {
   }

   @Override
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      if (plugin.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
         this.pp = PlayerPoints.getInstance().getAPI();
         if (this.pp == null) {
            throw new EconomyLoadException("Failed to hook into PlayerPoints economy");
         } else {
            SendMessage.infoMessage("Successfully hooked into PlayerPoints");
            String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.player-points");
            this.formatSingular(pathIgnoreCase);
            this.formatPlural(pathIgnoreCase);
            this.friendly = ChatUtil.formatColors(ConfigManager.getConfig().getString(pathIgnoreCase + ".friendly", "Points"));
            this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
         }
      } else {
         throw new EconomyLoadException("Could not find PlayerPoints");
      }
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      return this.pp.look(offlinePlayer.getUniqueId());
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      this.pp.give(offlinePlayer.getUniqueId(), (int)Math.round(balance));
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      this.pp.take(offlinePlayer.getUniqueId(), (int)Math.round(balance));
      return true;
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.PLAYER_POINTS);
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
