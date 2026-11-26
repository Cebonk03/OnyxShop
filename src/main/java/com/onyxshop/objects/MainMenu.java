package com.onyxshop.objects;

import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MainMenu extends ShopInventory {
   private final Inventory inv;
   private final OnyxShop plugin = OnyxShop.getInstance();
   private ScheduledTask tickTask;

   public static void open(Player var0) {
      OnyxShop var1 = OnyxShop.getInstance();
      if (var1.bedrock && var1.bedrockMenus && UserManager.getUser(var0).isBedrock() && var1.getBedrockInventorys().isEnabledFor(var0)) {
         var1.getBedrockInventorys().openMainMenu(var0);
      } else {
         new MainMenu(var0);
      }
   }

   public MainMenu(Player var1) {
      User var2 = UserManager.getUser(var1);
      this.inv = this.plugin.getMetaUtils().createInventory(this, CalculateAmount.getMainInvSlots(var2), TitleManager.get(TitleManager.Menu.MAIN_MENU, var1));
      this.init(var1);
      this.plugin.navBar.addMainMenuNavBar(this.inv, var1);
      this.open(var1, var2);
   }

   private void open(Player var1, User var2) {
      var2.setOpenNewGUI(true);
      var1.openInventory(this.inv);
   }

   private void init(Player var1) {
      this.plugin.getSectionItems().forEach((var2x, var3x) -> {
         ItemStack var4 = var3x.getItem(var1);
         if (!var3x.isHideIfNoPerms() || PermissionsCache.hasPermission(var1, "OnyxShop.shop." + var2x.toLowerCase())) {
            if (var3x.isRotating()) {
               this.startTicking(var3x, var4);
            }

            for (Integer var6 : var3x.getSlots()) {
               this.inv.setItem(var6, var4);
            }
         }
      });
      if (this.plugin.createItem.fillItem != null) {
         int var2 = this.plugin.mainMenuSize;

         for (int var3 = 0; var3 < (this.plugin.navBar.isEnableMainNav() ? var2 - 9 : var2); var3++) {
            if (this.inv.getItem(var3) == null) {
               this.inv.setItem(var3, this.plugin.createItem.fillItem);
            }
         }
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   private void startTicking(SectionItem var1, ItemStack var2) {
      Runnable var3 = var1.getTickTask(this.inv, var2);
      this.tickTask = OnyxShop.getInstance().runTaskTimer(() -> {
         if (this.inv.getViewers().isEmpty()) {
            this.tickTask.cancel();
         }

         var3.run();
      }, 0L, 20L);
   }
}
