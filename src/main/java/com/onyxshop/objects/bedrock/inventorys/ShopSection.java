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
      boolean var1,
      com.onyxshop.objects.shops.ShopSection var2,
      int var3,
      boolean var4,
      String var5,
      Map<Integer, ItemStack> var6,
      List<String> var7,
      ItemStack var8,
      Translatable var9,
      int var10,
      boolean var11
   ) {
      super(var1 ? var10 : countButtons(var7, var10, var11));
      this.section = var2;
      this.page = var3;
      this.disabledBackButton = var4;
      this.rootSection = var5;
      this.pageContents = var6;
      this.itemOrder = var7;
      this.fillItem = var8;
      this.title = var9;
      this.size = var10;
      this.navBarEnabled = var11;
   }

   private static int countButtons(List<String> var0, int var1, boolean var2) {
      int var3 = var2 ? var1 - 9 : var1;
      int var4 = 0;

      for (int var5 = 0; var5 < var3 && var5 < var0.size(); var5++) {
         if (var0.get(var5) != null) {
            var4++;
         }
      }

      return var4 + (var2 ? 9 : 0);
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
   public void openForm(OnyxShop var1, Player var2, FloodgatePlayer var3) {
      this.p = var2;
      Builder var4 = (Builder)SimpleForm.builder().title(this.title.getBedrockLegacy());
      this.buttonItemLoc = new String[this.items.length];
      int var5 = this.navBarEnabled ? this.size - 9 : this.size;
      int var6 = 0;

      for (int var7 = 0; var7 < var5 && var7 < this.itemOrder.size(); var7++) {
         String var8 = this.itemOrder.get(var7);
         ItemStack var9 = var8 == null ? null : this.pageContents.get(var7);
         if (var9 != null) {
            this.buttonItemLoc[var6] = var8;
            this.addItem(var4, var9, var6++, this.getBedrockTexture(var8));
         }
      }

      int var11 = var6;
      if (this.navBarEnabled) {
         ItemStack[] var12 = var1.navBar.getShopNavBar(this.getSection(), var2, this.page, this.getAllPages());
         String[] var13 = var1.navBar.getShopNavTextures(this.getSection(), this.page, this.getAllPages());

         for (int var10 = 0; var10 < var12.length; var10++) {
            if (var12[var10] != null) {
               this.addItem(var4, var12[var10], var6++, var13[var10]);
            }
         }
      }

      var4.validResultHandler(var5x -> {
         int var6x = var5x.clickedButtonId();
         if (this.items[var6x] != null) {
            if (this.navBarEnabled && var6x >= var11) {
               if (!var1.navBar.execute(this, var2, var6x - var11, this.page)) {
                  this.openForm(var1, var2, var3);
               }
            } else {
               if (!this.handleItemClick(var1, var2, var3, this.buttonItemLoc[var6x])) {
                  this.openForm(var1, var2, var3);
               }
            }
         }
      });
      var3.sendForm(var4);
   }

   @Override
   public void openGridForm(OnyxShop var1, Player var2, FloodgatePlayer var3, boolean var4) {
      this.p = var2;
      this.buildGridForm(var1, var2, var3, var4);
   }

   private void buildGridForm(OnyxShop var1, Player var2, FloodgatePlayer var3, boolean var4) {
      Builder var5 = (Builder)SimpleForm.builder().title(this.getTitle(this.title, this.size, 99, var4));
      int var6 = this.navBarEnabled ? this.size - 9 : this.size;

      for (int var7 = 0; var7 < var6; var7++) {
         ItemStack var8 = this.pageContents.get(var7);
         if (var8 == null) {
            if (this.fillItem != null) {
               this.addGridItem(var5, this.fillItem, true, var7, false);
            } else {
               var5.button("null");
            }
         } else {
            this.addGridItem(var5, var8, false, var7, false, this.getBedrockTexture(this.itemOrder.get(var7)));
         }
      }

      if (this.navBarEnabled) {
         int var11 = this.size - 9;
         ItemStack[] var12 = var1.navBar.getShopNavBar(this.getSection(), var2, this.page, this.getAllPages());
         String[] var9 = var1.navBar.getShopNavTextures(this.getSection(), this.page, this.getAllPages());

         for (int var10 = 0; var10 < var12.length; var10++) {
            if (var12[var10] != null && var12[var10].getType() != Material.AIR) {
               this.addGridItem(var5, var12[var10], false, var11, false, var9[var10]);
            } else {
               var5.button("null");
            }

            var11++;
         }
      }

      var5.validResultHandler(var6x -> {
         int var7x = var6x.clickedButtonId();
         if (this.items[var7x] != null) {
            this.handleClick(var5, var1, var2, var3, var7x, -1, var4);
         }
      });
      var3.sendForm(var5);
   }

   private void reopenGridForm(Builder var1, OnyxShop var2, Player var3, FloodgatePlayer var4, boolean var5, int var6, int var7) {
      if (var5 && var6 != var7) {
         var1 = (Builder)SimpleForm.builder().title(this.getTitle(this.title, this.size, var6, var5));

         for (int var8 = 0; var8 < this.items.length; var8++) {
            if (this.items[var8] != null) {
               this.readdGridItem(var1, var8, var6 == var8);
            } else {
               var1.button("null");
            }
         }

         var7 = var6;
      }

      int var10 = var7;
      Builder var9 = var1;
      var1.validResultHandler(var7x -> {
         int var8x = var7x.clickedButtonId();
         if (this.items[var8x] != null) {
            this.handleClick(var9, var2, var3, var4, var8x, var10, var5);
         }
      });
      var4.sendForm(var1);
   }

   private void handleClick(Builder var1, OnyxShop var2, Player var3, FloodgatePlayer var4, int var5, int var6, boolean var7) {
      if (var7 && var6 != var5) {
         this.reopenGridForm(var1, var2, var3, var4, var7, var5, var6);
      } else if (this.items[var5].isFillItem()) {
         this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
      } else if (this.navBarEnabled && var5 >= this.size - 9 && var5 <= this.size - 1) {
         if (!var2.navBar.execute(this, var3, var5 - (this.size - 9), this.page)) {
            this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
         }
      } else {
         if (!this.handleItemClick(var2, var3, var4, this.itemOrder.get(var5))) {
            this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
         }
      }
   }

   private String getBedrockTexture(String var1) {
      DisplayItem var2 = var1 == null ? null : this.section.getDisplayItem(this.getSection() + "." + var1);
      return var2 instanceof ShopItem ? ((ShopItem)var2).getBedrockTexture() : null;
   }

   private boolean handleItemClick(OnyxShop var1, Player var2, FloodgatePlayer var3, String var4) {
      if (var4 == null) {
         return false;
      } else {
         DisplayItem var5 = this.section.getDisplayItem(this.getSection() + "." + var4);
         if (var5 == null) {
            return false;
         } else if (var5 instanceof ActionItem) {
            return var1.navBar.execute(this, var2, ((ActionItem)var5).getAction(), this.page);
         } else if (var5.hasItemError()) {
            SendMessage.chatToPlayer(var2, Lang.ITEM_ERROR.get(var2));
            return false;
         } else if (!var5.canPurchase(var2, "shop", false) || !var1.menuHandler.isAllowedGamemode(var2)) {
            return false;
         } else if (!var5.meetsRequirements(var2, false)) {
            return false;
         } else {
            if (var5.isClickCommand() && var1.commandItems) {
               CommandHelper.executeCommandItem(var5, var2, CommandType.CLICK);
            }

            ShopItem var6 = (ShopItem)var5;
            if (var6.isLinked()) {
               if (PermissionsCache.hasPermission(var2, "OnyxShop.shop." + var6.getSubSection().toLowerCase())) {
                  var1.getSection(var6.getSubSection()).openShopSection(var2, this.disabledBackButton, var6.section);
               } else {
                  SendMessage.chatToPlayer(var2, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(var2));
               }

               return true;
            } else if (!var6.isDisplayItem() && (var6.isBuyAble() || var6.isSellAble())) {
               this.openItemActionForm(var1, var2, var3, var6);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private void openItemActionForm(OnyxShop var1, Player var2, FloodgatePlayer var3, ShopItem var4) {
      String var5 = var4.getDisplayname();
      if (var4.hasCustomName()) {
         var5 = var1.getMetaUtils().getDisplayName(var4.getShopItem().getItemMeta()).getBedrockLegacy();
      }

      Builder var6 = (Builder)SimpleForm.builder().title(var5);
      ArrayList<Runnable> var7 = new ArrayList<>();
      if (var4.isBuyAble()) {
         var6.button(Lang.BUY.get(var2).getBedrockLegacy());
         var7.add(() -> this.executeBuy(var1, var2, var4));
      }

      if (var4.isSellAble()) {
         var6.button(Lang.SELL.get(var2).getBedrockLegacy());
         var7.add(() -> this.executeSell(var1, var2, var4));
         if (var1.MMB) {
            var6.button(Lang.SELL_ALL.get(var2).getBedrockLegacy());
            var7.add(() -> this.executeSellAll(var1, var2, var4));
         }
      }

      var6.button(Lang.BACK.get(var2).getBedrockLegacy());
      var7.add(() -> this.refresh(var1, var2));
      var6.validResultHandler(var1x -> ((Runnable)var7.get(var1x.clickedButtonId())).run());
      var3.sendForm(var6);
   }

   private void executeBuy(OnyxShop var1, Player var2, ShopItem var3) {
      if (var3.getMaxBuy() == -1) {
         if (var3.isABuyPricing()) {
            var1.menuHandler.purchaseItemA(var2, var3);
         } else {
            var1.menuHandler.purchaseItem(var2, var3);
         }

         this.refresh(var1, var2);
      } else {
         new com.onyxshop.objects.TransactionMenu(
               var2, var3, this.rootSection, this.disabledBackButton, Transaction.Mode.BUY, Transaction.Type.BUY_SCREEN, 1
            )
            .open(false);
      }
   }

   private void executeSell(OnyxShop var1, Player var2, ShopItem var3) {
      if (var3.getMaxSell() == -1) {
         if (var3.isASellPricing()) {
            var1.menuHandler.sellItemA(var2, var3, var3.getStackSize());
         } else {
            var1.menuHandler.sellItem(var2, var3, var3.getStackSize());
         }

         this.refresh(var1, var2);
      } else {
         new com.onyxshop.objects.TransactionMenu(
               var2, var3, this.rootSection, this.disabledBackButton, Transaction.Mode.SELL, Transaction.Type.SELL_SCREEN, 1
            )
            .open(false);
      }
   }

   private void executeSellAll(OnyxShop var1, Player var2, ShopItem var3) {
      if (var3.isASellPricing()) {
         var1.menuHandler.sellItemA(var2, var3, Integer.MAX_VALUE);
      } else {
         var1.menuHandler.sellItem(var2, var3, Integer.MAX_VALUE);
      }

      this.refresh(var1, var2);
   }

   private void refresh(OnyxShop var1, Player var2) {
      var1.getSection(this.getSection()).openShopSection(var2, this.page, this.disabledBackButton, this.rootSection);
   }
}
