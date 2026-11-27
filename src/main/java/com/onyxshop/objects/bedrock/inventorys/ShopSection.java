package com.onyxshop.objects.bedrock.inventorys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.bedrock.BedrockInventory;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.Transaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.SimpleForm.Builder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class ShopSection extends BedrockInventory {
   private final com.onyxshop.objects.shops.ShopSection section;
   private final int page;
   private final boolean disabledBackButton;
   private final String rootSection;
   private final Map<Integer, ItemStack> pageContents;
   private final List<String> itemOrder;
   private final ItemStack fillItem;
   private final Translatable title;
   private final int size;
   private final boolean navBarEnabled;
   private String[] buttonItemLoc;

   public ShopSection(
      boolean flag,
      com.onyxshop.objects.shops.ShopSection section,
      int value,
      boolean flag2,
      String text,
      Map<Integer, ItemStack> map,
      List<String> values,
      ItemStack itemStack,
      Translatable text2,
      int value2,
      boolean flag3
   ) {
      super(flag ? value2 : countButtons(values, value2, flag3));
      this.section = section;
      this.page = value;
      this.disabledBackButton = flag2;
      this.rootSection = text;
      this.pageContents = map;
      this.itemOrder = values;
      this.fillItem = itemStack;
      this.title = text2;
      this.size = value2;
      this.navBarEnabled = flag3;
   }

   private static int countButtons(List<String> values, int amount, boolean flag) {
      int value = flag ? amount - 9 : amount;
      int value2 = 0;

      for (int size = 0; size < value && size < values.size(); size++) {
         if (values.get(size) != null) {
            value2++;
         }
      }

      return value2 + (flag ? 9 : 0);
   }

   public String getSection() {
      return this.section.getSection();
   }

   public int getPage() {
      return this.page;
   }

   public int getAllPages() {
      return this.section.getPages();
   }

   public boolean isDisabledBackButton() {
      return this.disabledBackButton;
   }

   public boolean isSubSection() {
      return this.rootSection != null;
   }

   public String getRootSection() {
      return this.rootSection;
   }

   @Override
   public void openForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer) {
      this.p = player;
      Builder builder = (Builder)SimpleForm.builder().title(this.title.getBedrockLegacy());
      this.buttonItemLoc = new String[this.items.length];
      int size = this.navBarEnabled ? this.size - 9 : this.size;
      int value = 0;

      for (int index = 0; index < size && index < this.itemOrder.size(); index++) {
         String itemId = this.itemOrder.get(index);
         ItemStack itemStack = itemId == null ? null : this.pageContents.get(index);
         if (itemStack != null) {
            this.buttonItemLoc[value] = itemId;
            this.addItem(builder, itemStack, value++, this.getBedrockTexture(itemId));
         }
      }

      int value2 = value;
      if (this.navBarEnabled) {
         ItemStack[] shopNavBar = plugin.navBar.getShopNavBar(this.getSection(), player, this.page, this.getAllPages());
         String[] shopNavTextures = plugin.navBar.getShopNavTextures(this.getSection(), this.page, this.getAllPages());

         for (int index = 0; index < shopNavBar.length; index++) {
            if (shopNavBar[index] != null) {
               this.addItem(builder, shopNavBar[index], value++, shopNavTextures[index]);
            }
         }
      }

      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            if (this.navBarEnabled && id >= value2) {
               if (!plugin.navBar.execute(this, player, id - value2, this.page)) {
                  this.openForm(plugin, player, floodgatePlayer);
               }
            } else {
               if (!this.handleItemClick(plugin, player, floodgatePlayer, this.buttonItemLoc[id])) {
                  this.openForm(plugin, player, floodgatePlayer);
               }
            }
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   @Override
   public void openGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      this.p = player;
      this.buildGridForm(plugin, player, floodgatePlayer, flag);
   }

   private void buildGridForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag) {
      Builder builder = (Builder)SimpleForm.builder().title(this.getTitle(this.title, this.size, 99, flag));
      int size = this.navBarEnabled ? this.size - 9 : this.size;

      for (int index = 0; index < size; index++) {
         ItemStack itemStack = this.pageContents.get(index);
         if (itemStack == null) {
            if (this.fillItem != null) {
               this.addGridItem(builder, this.fillItem, true, index, false);
            } else {
               builder.button("null");
            }
         } else {
            this.addGridItem(builder, itemStack, false, index, false, this.getBedrockTexture(this.itemOrder.get(index)));
         }
      }

      if (this.navBarEnabled) {
         int value = this.size - 9;
         ItemStack[] shopNavBar = plugin.navBar.getShopNavBar(this.getSection(), player, this.page, this.getAllPages());
         String[] shopNavTextures = plugin.navBar.getShopNavTextures(this.getSection(), this.page, this.getAllPages());

         for (int index = 0; index < shopNavBar.length; index++) {
            if (shopNavBar[index] != null && shopNavBar[index].getType() != Material.AIR) {
               this.addGridItem(builder, shopNavBar[index], false, value, false, shopNavTextures[index]);
            } else {
               builder.button("null");
            }

            value++;
         }
      }

      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder, plugin, player, floodgatePlayer, id, -1, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void reopenGridForm(Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, boolean flag, int value, int value2) {
      if (flag && value != value2) {
         builder = (Builder)SimpleForm.builder().title(this.getTitle(this.title, this.size, value, flag));

         for (int index = 0; index < this.items.length; index++) {
            if (this.items[index] != null) {
               this.readdGridItem(builder, index, value == index);
            } else {
               builder.button("null");
            }
         }

         value2 = value;
      }

      int value3 = value2;
      Builder builder2 = builder;
      builder.validResultHandler(element -> {
         int id = element.clickedButtonId();
         if (this.items[id] != null) {
            this.handleClick(builder2, plugin, player, floodgatePlayer, id, value3, flag);
         }
      });
      floodgatePlayer.sendForm(builder);
   }

   private void handleClick(Builder builder, OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, int value, int value2, boolean flag) {
      if (flag && value2 != value) {
         this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, value, value2);
      } else if (this.items[value].isFillItem()) {
         this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
      } else if (this.navBarEnabled && value >= this.size - 9 && value <= this.size - 1) {
         if (!plugin.navBar.execute(this, player, value - (this.size - 9), this.page)) {
            this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
         }
      } else {
         if (!this.handleItemClick(plugin, player, floodgatePlayer, this.itemOrder.get(value))) {
            this.reopenGridForm(builder, plugin, player, floodgatePlayer, flag, 99, value2);
         }
      }
   }

   private String getBedrockTexture(String text) {
      DisplayItem displayItem = text == null ? null : this.section.getDisplayItem(this.getSection() + "." + text);
      return displayItem instanceof ShopItem ? ((ShopItem)displayItem).getBedrockTexture() : null;
   }

   private boolean handleItemClick(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, String itemId) {
      if (itemId == null) {
         return false;
      } else {
         DisplayItem displayItem = this.section.getDisplayItem(this.getSection() + "." + itemId);
         if (displayItem == null) {
            return false;
         } else if (displayItem instanceof ActionItem) {
            return plugin.navBar.execute(this, player, ((ActionItem)displayItem).getAction(), this.page);
         } else if (displayItem.hasItemError()) {
            SendMessage.chatToPlayer(player, Lang.ITEM_ERROR.get(player));
            return false;
         } else if (!displayItem.canPurchase(player, "shop", false) || !plugin.menuHandler.isAllowedGamemode(player)) {
            return false;
         } else if (!displayItem.meetsRequirements(player, false)) {
            return false;
         } else {
            if (displayItem.isClickCommand() && plugin.commandItems) {
               CommandHelper.executeCommandItem(displayItem, player, CommandType.CLICK);
            }

            ShopItem shopItem = (ShopItem)displayItem;
            if (shopItem.isLinked()) {
               if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.getSubSection().toLowerCase())) {
                  plugin.getSection(shopItem.getSubSection()).openShopSection(player, this.disabledBackButton, shopItem.section);
               } else {
                  SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(player));
               }

               return true;
            } else if (!shopItem.isDisplayItem() && (shopItem.isBuyAble() || shopItem.isSellAble())) {
               this.openItemActionForm(plugin, player, floodgatePlayer, shopItem);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private void openItemActionForm(OnyxShop plugin, Player player, FloodgatePlayer floodgatePlayer, ShopItem shopItem) {
      String displayname = shopItem.getDisplayname();
      if (shopItem.hasCustomName()) {
         displayname = plugin.getMetaUtils().getDisplayName(shopItem.getShopItem().getItemMeta()).getBedrockLegacy();
      }

      Builder builder = (Builder)SimpleForm.builder().title(displayname);
      ArrayList<Runnable> values = new ArrayList<>();
      if (shopItem.isBuyAble()) {
         builder.button(Lang.BUY.get(player).getBedrockLegacy());
         values.add(() -> this.executeBuy(plugin, player, shopItem));
      }

      if (shopItem.isSellAble()) {
         builder.button(Lang.SELL.get(player).getBedrockLegacy());
         values.add(() -> this.executeSell(plugin, player, shopItem));
         if (plugin.MMB) {
            builder.button(Lang.SELL_ALL.get(player).getBedrockLegacy());
            values.add(() -> this.executeSellAll(plugin, player, shopItem));
         }
      }

      builder.button(Lang.BACK.get(player).getBedrockLegacy());
      values.add(() -> this.refresh(plugin, player));
      builder.validResultHandler(element -> ((Runnable)values.get(element.clickedButtonId())).run());
      floodgatePlayer.sendForm(builder);
   }

   private void executeBuy(OnyxShop plugin, Player player, ShopItem shopItem) {
      if (shopItem.getMaxBuy() == -1) {
         if (shopItem.isABuyPricing()) {
            plugin.menuHandler.purchaseItemA(player, shopItem);
         } else {
            plugin.menuHandler.purchaseItem(player, shopItem);
         }

         this.refresh(plugin, player);
      } else {
         new com.onyxshop.objects.TransactionMenu(
               player, shopItem, this.rootSection, this.disabledBackButton, Transaction.Mode.BUY, Transaction.Type.BUY_SCREEN, 1
            )
            .open(false);
      }
   }

   private void executeSell(OnyxShop plugin, Player player, ShopItem shopItem) {
      if (shopItem.getMaxSell() == -1) {
         if (shopItem.isASellPricing()) {
            plugin.menuHandler.sellItemA(player, shopItem, shopItem.getStackSize());
         } else {
            plugin.menuHandler.sellItem(player, shopItem, shopItem.getStackSize());
         }

         this.refresh(plugin, player);
      } else {
         new com.onyxshop.objects.TransactionMenu(
               player, shopItem, this.rootSection, this.disabledBackButton, Transaction.Mode.SELL, Transaction.Type.SELL_SCREEN, 1
            )
            .open(false);
      }
   }

   private void executeSellAll(OnyxShop plugin, Player player, ShopItem shopItem) {
      if (shopItem.isASellPricing()) {
         plugin.menuHandler.sellItemA(player, shopItem, Integer.MAX_VALUE);
      } else {
         plugin.menuHandler.sellItem(player, shopItem, Integer.MAX_VALUE);
      }

      this.refresh(plugin, player);
   }

   private void refresh(OnyxShop plugin, Player player) {
      plugin.getSection(this.getSection()).openShopSection(player, this.page, this.disabledBackButton, this.rootSection);
   }
}
