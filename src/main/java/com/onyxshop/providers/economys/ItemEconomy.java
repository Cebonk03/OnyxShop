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

   public ItemEconomy(String itemId, String text, int value) {
      this.itemFormat = new ItemFormat(itemId, text, value);
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
   public void setup(OnyxShop plugin) throws EconomyLoadException {
      this.plugin = plugin;
      if (this.itemFormat.getItem() == null) {
         String errorMessage = this.itemFormat.getErrorMessage();
         if (errorMessage.contains("Item provider not enabled yet!")) {
            this.wait = true;
         } else {
            throw new EconomyLoadException(errorMessage);
         }
      } else {
         this.item = this.itemFormat.getItem();
         this.iTags = this.itemFormat.getiTags();

         try {
            if (XMaterial.matchXMaterial(this.item.getType()) == XMaterial.PLAYER_HEAD) {
               this.iTags.add("SkullOwner::Id");
            }
         } catch (IllegalArgumentException exception) {
         }

         SendMessage.infoMessage("Successfully loaded item economy '" + this.itemFormat.getId() + "'");
         if (!this.iTags.isEmpty()) {
            SendMessage.logDebugMessage("Ignored tags: " + String.join(", ", this.iTags));
         }

         String pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.item:" + this.itemFormat.getId());
         if (!ConfigManager.getConfig().contains(pathIgnoreCase)) {
            pathIgnoreCase = ConfigManager.getPathIgnoreCase("currency-formats.item");
         }

         String materialName = plugin.getMaterialName(this.item.getType().toString());
         String itemId = this.item.hasItemMeta() && this.item.getItemMeta().hasDisplayName() ? this.item.getItemMeta().getDisplayName() : materialName;
         this.formatSingular(pathIgnoreCase, itemId, materialName);
         this.formatPlural(pathIgnoreCase, itemId, materialName);
         this.RGB = ChatUtil.hasRGB(this.singular) || ChatUtil.hasRGB(this.plural);
         this.friendly = ChatUtil.formatColors(
            ConfigManager.getConfig().getString(pathIgnoreCase + ".friendly", itemId).replace("%material-name%", materialName).replace("%item-name%", itemId)
         );
         this.priceFormat = PriceFormatter.getFormatter(pathIgnoreCase);
      }
   }

   @Override
   public double getBalance(OfflinePlayer offlinePlayer) {
      try {
         return this.getItems((Player)offlinePlayer);
      } catch (ClassCastException exception) {
         SendMessage.warnMessage("Error occurred while retrieving item balance from " + offlinePlayer.getName());
         exception.printStackTrace();
         return 0.0;
      }
   }

   private int getItems(Player player) {
      int value = 0;

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack != null && !itemStack.getType().equals(Material.AIR) && this.equals(itemStack)) {
            value += itemStack.getAmount();
         }
      }

      return value;
   }

   @Override
   public void depositBalance(OfflinePlayer offlinePlayer, double balance) {
      try {
         HashMap<Integer, ItemStack> map = ((Player)offlinePlayer).getInventory().addItem(this.getItems(Math.round(balance)));
         if (!map.isEmpty()) {
            for (ItemStack itemStack : map.values()) {
               ((Player)offlinePlayer).getWorld().dropItem(((Player)offlinePlayer).getLocation(), itemStack);
            }

            SendMessage.chatToPlayer((Player)offlinePlayer, Lang.NOT_ENOUGH_SPACE_INSIDE_INVENTORY.get());
         }
      } catch (ClassCastException exception) {
         SendMessage.warnMessage("Error occurred while depositing item economy to " + offlinePlayer.getName());
         exception.printStackTrace();
      }
   }

   private ItemStack[] getItems(double amount) {
      ItemStack[] itemIds = new ItemStack[(int)Math.ceil(amount / this.item.getMaxStackSize())];
      int value = (int)Math.round(amount);

      for (int index = 0; index < itemIds.length; index++) {
         int size = Math.min(value, this.item.getMaxStackSize());
         ItemStack itemStack = new ItemStack(this.item);
         itemStack.setAmount(size);
         itemIds[index] = itemStack;
         value -= size;
      }

      return itemIds;
   }

   @Override
   public boolean withdrawBalance(OfflinePlayer offlinePlayer, double balance) {
      int value = (int)Math.round(balance);
      try {
         for (int index = 0; index < 36; index++) {
            ItemStack itemStack = ((Player)offlinePlayer).getInventory().getItem(index);
            if (itemStack != null && !itemStack.getType().equals(Material.AIR) && this.equals(itemStack)) {
               if (itemStack.getAmount() > value) {
                  itemStack.setAmount(itemStack.getAmount() - value);
                  value = 0;
                  break;
               }

               value -= itemStack.getAmount();
               ((Player)offlinePlayer).getInventory().clear(index);
            }
         }
      } catch (ClassCastException exception) {
         SendMessage.warnMessage("Error occurred while withdrawing item economy from " + offlinePlayer.getName());
         exception.printStackTrace();
         return false;
      }

      return value <= 0;
   }

   private boolean equals(ItemStack itemStack) {
      return this.iTags.isEmpty() ? this.item.isSimilar(itemStack) : this.plugin.versionHandler.isSimilar(itemStack, this.item, this.iTags);
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

   @Override
   public String getPath() {
      return this.itemFormat.getPath();
   }

   public ItemStack getItem() {
      return this.item;
   }

   private void formatSingular(String text, String text2, String text3) {
      String text4 = ConfigManager.getConfig().getString(text + ".singular", text2);
      text4 = ChatUtil.formatColors(text4.replace("%material-name%", text3).replace("%item-name%", text2));
      if (this.isSymbol(ChatColor.stripColor(text4))) {
         this.singular = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text4).getLegacy();
      } else {
         this.singular = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text4).getLegacy();
      }
   }

   private void formatPlural(String text, String text2, String text3) {
      String text4 = ConfigManager.getConfig().getString(text + ".plural", text2);
      text4 = ChatUtil.formatColors(text4.replace("%material-name%", text3).replace("%item-name%", text2));
      if (this.isSymbol(ChatColor.stripColor(text4))) {
         this.plural = Lang.SYMBOL_PRICING_FORMAT.get().replace("%symbol%", text4).getLegacy();
      } else {
         this.plural = Lang.NAMED_PRICING_FORMAT.get().replace("%currency-format%", text4).getLegacy();
      }
   }

   private boolean isSymbol(String text) {
      return text.chars().count() == 1L;
   }
}
