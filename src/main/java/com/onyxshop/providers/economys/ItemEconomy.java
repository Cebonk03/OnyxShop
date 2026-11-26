package com.onyxshop.providers.economys;

import java.util.HashMap;
import java.util.List;
import com.onyxshop.util.exceptions.ItemLoadException;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.delayed.DelayedItemProvider;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.EconomyProvider;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.economys.formatter.PriceFormatter;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.ItemFormat;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.EconomyLoadException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemEconomy implements EconomyProvider, DelayedItemProvider {
   private ItemStack item;
   private List<String> iTags;
   private final ItemFormat itemFormat;
   private String friendly;
   private String singular;
   private String plural;
   private OnyxShop plugin;
   private boolean skull;
   private final boolean decimal = false;
   private boolean wait = false;
   private boolean RGB;
   private PriceFormatter.Format priceFormat;

   public ItemEconomy(String var1, String var2, int var3) {
      this.itemFormat = new ItemFormat(var1, var2, var3);
   }

   @Override
   public void load() throws EconomyLoadException {
      this.itemFormat.load();
      if (this.itemFormat.getItem() == null) {
         throw new EconomyLoadException(this.itemFormat.getErrorMessage());
      } else {
         this.wait = false;
         this.setup(this.plugin);
      }
   }

   @Override
   public void setup(OnyxShop var1) throws EconomyLoadException {
      this.plugin = var1;
      if (this.itemFormat.getItem() == null) {
         String var6 = this.itemFormat.getErrorMessage();
         if (var6.contains("Item provider not enabled yet!")) {
            this.wait = true;
         } else {
            throw new EconomyLoadException(var6);
         }
      } else {
         this.item = this.itemFormat.getItem();
         this.iTags = this.itemFormat.getiTags();

         try {
            if (XMaterial.matchXMaterial(this.item.getType()) == XMaterial.PLAYER_HEAD) {
               this.iTags.add("SkullOwner::Id");
            }
         } catch (IllegalArgumentException var5) {
         }

         SendMessage.infoMessage("Successfully loaded item economy '" + this.itemFormat.getId() + "'");
         if (!this.iTags.isEmpty()) {
            SendMessage.logDebugMessage("Ignored tags: " + String.join(", ", this.iTags));
         }

         String var2 = ConfigManager.getPathIgnoreCase("currency-formats.item:" + this.itemFormat.getId());
         if (!ConfigManager.getConfig().contains(var2)) {
            var2 = ConfigManager.getPathIgnoreCase("currency-formats.item");
         }

         String var3 = var1.getMaterialName(this.item.getType().toString());
         String var4 = this.item.hasItemMeta() && this.item.getItemMeta().hasDisplayName() ? this.item.getItemMeta().getDisplayName() : var3;
         this.formatSingular(var2, var4, var3);
         this.formatPlural(var2, var4, var3);
         this.RGB = ChatUtil.hasRGB(this.singular) || ChatUtil.hasRGB(this.plural);
         this.friendly = ChatUtil.formatColors(
            ConfigManager.getConfig().getString(var2 + ".friendly", var4).replace("%material-name%", var3).replace("%item-name%", var4)
         );
         this.priceFormat = PriceFormatter.getFormatter(var2);
      }
   }

   @Override
   public double getBalance(OfflinePlayer var1) {
      try {
         return this.getItems((Player)var1);
      } catch (ClassCastException var3) {
         SendMessage.warnMessage("Error occurred while retrieving item balance from " + var1.getName());
         var3.printStackTrace();
         return 0.0;
      }
   }

   private int getItems(Player var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var1.getInventory().getItem(var3);
         if (var4 != null && !var4.getType().equals(Material.AIR) && this.equals(var4)) {
            var2 += var4.getAmount();
         }
      }

      return var2;
   }

   @Override
   public void depositBalance(OfflinePlayer var1, double var2) {
      try {
         HashMap<Integer, ItemStack> var4 = ((Player)var1).getInventory().addItem(this.getItems(Math.round(var2)));
         if (!var4.isEmpty()) {
            for (ItemStack var6 : var4.values()) {
               ((Player)var1).getWorld().dropItem(((Player)var1).getLocation(), var6);
            }

            SendMessage.chatToPlayer((Player)var1, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
         }
      } catch (ClassCastException var7) {
         SendMessage.warnMessage("Error occurred while depositing item economy to " + var1.getName());
         var7.printStackTrace();
      }
   }

   private ItemStack[] getItems(double var1) {
      ItemStack[] var3 = new ItemStack[(int)Math.ceil(var1 / this.item.getMaxStackSize())];
      int var4 = (int)Math.round(var1);

      for (int var5 = 0; var5 < var3.length; var5++) {
         int var6 = Math.min(var4, this.item.getMaxStackSize());
         ItemStack var7 = new ItemStack(this.item);
         var7.setAmount(var6);
         var3[var5] = var7;
         var4 -= var6;
      }

      return var3;
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer var1, double var2) {
      int var4 = (int)Math.round(var2);
      try {
         for (int var5 = 0; var5 < 36; var5++) {
            ItemStack var6 = ((Player)var1).getInventory().getItem(var5);
            if (var6 != null && !var6.getType().equals(Material.AIR) && this.equals(var6)) {
               if (var6.getAmount() > var4) {
                  var6.setAmount(var6.getAmount() - var4);
                  var4 = 0;
                  break;
               }

               var4 -= var6.getAmount();
               ((Player)var1).getInventory().clear(var5);
            }
         }
      } catch (ClassCastException var7) {
         SendMessage.warnMessage("Error occurred while withdrawing item economy from " + var1.getName());
         var7.printStackTrace();
         return false;
      }

      return var4 <= 0;
   }

   private boolean equals(ItemStack var1) {
      return this.iTags.isEmpty() ? this.item.isSimilar(var1) : this.plugin.versionHandler.isSimilar(var1, this.item, this.iTags);
   }

   @Override
   public EcoType getType() {
      return new EcoType(EconomyType.ITEM, this.itemFormat.getFormat());
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

   @Override
   public String getPath() {
      return this.itemFormat.getPath();
   }

   public ItemStack getItem() {
      return this.item;
   }

   private void formatSingular(String var1, String var2, String var3) {
      String var4 = ConfigManager.getConfig().getString(var1 + ".singular", var2);
      var4 = ChatUtil.formatColors(var4.replace("%material-name%", var3).replace("%item-name%", var2));
      if (this.isSymbol(ChatColor.stripColor(var4))) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var4).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var4).getLegacy();
      }
   }

   private void formatPlural(String var1, String var2, String var3) {
      String var4 = ConfigManager.getConfig().getString(var1 + ".plural", var2);
      var4 = ChatUtil.formatColors(var4.replace("%material-name%", var3).replace("%item-name%", var2));
      if (this.isSymbol(ChatColor.stripColor(var4))) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", var4).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", var4).getLegacy();
      }
   }

   private boolean isSymbol(String var1) {
      return var1.chars().count() == 1L;
   }
}
