package com.onyxshop.objects.bedrock.inventorys;

import java.util.LinkedHashSet;
import java.util.Set;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.SectionItem;
import com.onyxshop.objects.bedrock.BedrockInventory;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.SimpleForm.Builder;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

public class MainMenu extends BedrockInventory {
   private static final Set<String> SECTIONS = getSections();

   public MainMenu(OnyxShop var1, boolean var2) {
      super(var2 ? var1.mainMenuSize : SECTIONS.size() + (var1.navBar.isEnableMainNav() ? 9 : 0));
   }

   @Override
   public void openForm(OnyxShop var1, Player var2, FloodgatePlayer var3) {
      this.p = var2;
      Builder var4 = (Builder)SimpleForm.builder().title(TitleManager.get(TitleManager.Menu.MAIN_MENU, var2).getBedrockLegacy());
      String[] var5 = new String[this.items.length];
      int var6 = 0;

      for (String var8 : SECTIONS) {
         SectionItem var9 = var1.getSectionItem(var8);
         if (var9 != null && (!var9.isHideIfNoPerms() || PermissionsCache.hasPermission(var2, "OnyxShop.shop." + var8.toLowerCase()))) {
            var5[var6] = var8;
            this.addItem(var4, var9.getItem(var2), var6++, var9.getBedrockTexture());
         }
      }

      int var11 = var6;
      if (var1.navBar.isEnableMainNav()) {
         ItemStack[] var12 = var1.navBar.getMainMenuNavBar(var2);
         String[] var13 = var1.navBar.getMainMenuNavTextures();

         for (int var10 = 0; var10 < var12.length; var10++) {
            if (var12[var10] != null) {
               this.addItem(var4, var12[var10], var6++, var13[var10]);
            }
         }
      }

      var4.validResultHandler(var6x -> {
         int var7 = var6x.clickedButtonId();
         if (this.items[var7] != null) {
            if (!var1.navBar.isEnableMainNav() || var7 < var11) {
               String var8x = var5[var7];
               if (var8x != null && var1.getSections().containsKey(var8x)) {
                  if (PermissionsCache.hasPermission(var2, "OnyxShop.shop." + var8x.toLowerCase())) {
                     com.onyxshop.objects.shops.ShopSection var9x = var1.getSection(var8x);
                     if (!var9x.isCommandItem()) {
                        var9x.openShopSection(var2, false);
                     } else {
                        var9x.getClickCommands().forEach(var1xx -> CommandHelper.executeCommand(null, var2, var1xx));
                        if (!var9x.isCloseMenu()) {
                           this.openForm(var1, var2, var3);
                        }
                     }

                     var1.getSoundEffects().openShop(var2);
                  } else if (!var1.getSectionItem(var8x).isHideIfNoPerms()) {
                     SendMessage.chatToPlayer(var2, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(var2));
                  }
               }
            } else if (!var1.navBar.execute(this, var2, var7 - var11)) {
               this.openForm(var1, var2, var3);
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
      Builder var5 = (Builder)SimpleForm.builder().title(this.getTitle(TitleManager.get(TitleManager.Menu.MAIN_MENU, var2), this.items.length, 99, var4));

      for (int var6 = 0; var6 < (var1.navBar.isEnableMainNav() ? this.items.length - 9 : this.items.length); var6++) {
         String var7 = var1.getMainMenuSectionForSlot(var6);
         SectionItem var8 = var7 == null ? null : var1.getSectionItem(var7);
         if (var7 == null || var8 == null || var8.isHideIfNoPerms() && !PermissionsCache.hasPermission(var2, "OnyxShop.shop." + var7.toLowerCase())) {
            if (var1.createItem.fillItem != null && var6 < (var1.navBar.isEnableMainNav() ? this.items.length - 9 : this.items.length)) {
               this.addGridItem(var5, var1.createItem.fillItem, true, var6, false);
            } else {
               var5.button("null");
            }
         } else {
            this.addGridItem(var5, var8.getItem(var2), false, var6, false, var8.getBedrockTexture());
         }
      }

      if (var1.navBar.isEnableMainNav()) {
         int var10 = this.items.length - 9;
         ItemStack[] var11 = var1.navBar.getMainMenuNavBar(var2);
         String[] var12 = var1.navBar.getMainMenuNavTextures();

         for (int var9 = 0; var9 < var11.length; var9++) {
            if (var11[var9] != null) {
               this.addGridItem(var5, var11[var9], false, var10, false, var12[var9]);
            } else {
               var5.button("null");
            }

            var10++;
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
         var1 = (Builder)SimpleForm.builder().title(this.getTitle(TitleManager.get(TitleManager.Menu.MAIN_MENU, var3), this.items.length, var6, var5));

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
      } else {
         if (this.items[var5].isFillItem()) {
            this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
         }

         if (!var2.navBar.isEnableMainNav() || var5 < this.items.length - 9 || var5 > this.items.length - 1) {
            String var8 = var2.getMainMenuSectionForSlot(var5);
            if (var8 != null && var2.getSections().containsKey(var8)) {
               if (PermissionsCache.hasPermission(var3, "OnyxShop.shop." + var8.toLowerCase())) {
                  com.onyxshop.objects.shops.ShopSection var9 = var2.getSection(var8);
                  if (!var9.isCommandItem()) {
                     var9.openShopSection(var3, false);
                  } else {
                     var9.getClickCommands().forEach(var1x -> CommandHelper.executeCommand(null, var3, var1x));
                     if (!var9.isCloseMenu()) {
                        this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
                     }
                  }

                  var2.getSoundEffects().openShop(var3);
               } else if (!var2.getSectionItem(var8).isHideIfNoPerms()) {
                  SendMessage.chatToPlayer(var3, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get(var3));
               }
            }
         } else if (!var2.navBar.execute(this, var3, var5 - (this.items.length - 9))) {
            this.reopenGridForm(var1, var2, var3, var4, var7, 99, var6);
         }
      }
   }

   private static Set<String> getSections() {
      LinkedHashSet var0 = new LinkedHashSet();

      for (int var1 = 0; var1 < OnyxShop.getInstance().mainMenuSize; var1++) {
         String var2 = OnyxShop.getInstance().getMainMenuSectionForSlot(var1);
         if (var2 != null) {
            var0.add(var2);
         }
      }

      return var0;
   }
}
