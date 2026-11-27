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

   public static void open(Player player) {
      OnyxShop plugin = OnyxShop.getInstance();
      if (plugin.bedrock && plugin.bedrockMenus && UserManager.getUser(player).isBedrock() && plugin.getBedrockInventorys().isEnabledFor(player)) {
         plugin.getBedrockInventorys().openMainMenu(player);
      } else {
         new MainMenu(player);
      }
   }

   public MainMenu(Player player) {
      User user = UserManager.getUser(player);
      this.inv = this.plugin.getMetaUtils().createInventory(this, CalculateAmount.getMainInvSlots(user), TitleManager.get(TitleManager.Menu.MAIN_MENU, player));
      this.init(player);
      this.plugin.navBar.addMainMenuNavBar(this.inv, player);
      this.open(player, user);
   }

   private void open(Player player, User user) {
      user.setOpenNewGUI(true);
      player.openInventory(this.inv);
   }

   private void init(Player player) {
      this.plugin.getSectionItems().forEach((key, value) -> {
         ItemStack itemStack = value.getItem(player);
         if (!value.isHideIfNoPerms() || PermissionsCache.hasPermission(player, "OnyxShop.shop." + key.toLowerCase())) {
            if (value.isRotating()) {
               this.startTicking(value, itemStack);
            }

            for (Integer value2 : value.getSlots()) {
               this.inv.setItem(value2, itemStack);
            }
         }
      });
      if (this.plugin.createItem.fillItem != null) {
         int size = this.plugin.mainMenuSize;

         for (int index = 0; index < (this.plugin.navBar.isEnableMainNav() ? size - 9 : size); index++) {
            if (this.inv.getItem(index) == null) {
               this.inv.setItem(index, this.plugin.createItem.fillItem);
            }
         }
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   private void startTicking(SectionItem sectionItem, ItemStack itemStack) {
      Runnable task = sectionItem.getTickTask(this.inv, itemStack);
      this.tickTask = OnyxShop.getInstance().runTaskTimer(() -> {
         if (this.inv.getViewers().isEmpty()) {
            this.tickTask.cancel();
         }

         task.run();
      }, 0L, 20L);
   }
}
