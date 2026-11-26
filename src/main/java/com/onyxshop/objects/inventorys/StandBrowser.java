package com.onyxshop.objects.inventorys;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.objects.stands.Stand;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StandBrowser extends ShopInventory {
   private final Inventory inv;
   private final StandBrowser.Listen listener = new StandBrowser.Listen();
   private final OnyxShop plugin;
   private final ArrayList<Stand> stands;
   private int page = 1;
   private final int allPages;

   public StandBrowser(OnyxShop var1, Player var2) {
      this.stands = var1.getStandProvider().getStands().stream().sorted(Comparator.comparingInt(Stand::getId)).collect(Collectors.toCollection(ArrayList::new));
      int var3 = this.stands.size();
      this.allPages = (int)Math.ceil(var3 / 45.0);
      this.inv = var1.getMetaUtils().createInventory(this, CalculateAmount.getSlots(var3), TitleManager.get(TitleManager.Menu.SHOPSTAND_BROWSER, var2));
      this.plugin = var1;
      this.buildPane(var2);
      this.openPage(var2, 1);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      var2.openInventory(this.inv);
   }

   private void buildPane(Player var1) {
      this.inv
         .setItem(
            this.inv.getSize() - 5,
            new ItemBuilder(XMaterial.COMPASS.parseItem())
               .withDisplayName(
                  Lang.CURRENT_PAGE.get(var1).builder().append(" ").append(String.valueOf(this.page)).append("/").append(String.valueOf(this.allPages))
                     + " "
                     + this.page
                     + "/"
                     + this.allPages
               )
               .build()
         );
      this.inv
         .setItem(this.inv.getSize() - 6, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.PREVIOUS_PAGE.get(var1)).build());
      this.inv
         .setItem(
            this.inv.getSize() - 4, new ItemBuilder(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.NEXT_PAGE.get(var1)).build()
         );
   }

   private void openPage(Player var1, int var2) {
      int var3 = var2 * 45 - 45;

      for (int var4 = var3; var4 < var3 + Math.min(this.stands.size() - var3, 45); var4++) {
         Stand var5 = this.stands.get(var4);
         this.inv
            .addItem(
               new ItemStack[]{
                  new ItemBuilder(XMaterial.NETHER_STAR.parseItem())
                     .withDisplayName(Lang.SHOP_STAND_INFO_ITEM_NAME.get(var1))
                     .withCLore(
                        Lang.SHOP_STAND_INFO_ITEM_LORE
                           .get(var1)
                           .replace("%id%", String.valueOf(var5.getId()))
                           .replace("%location%", var5.getLoc().toString())
                           .replace("%item%", var5.getItem())
                           .replace("%type%", var5.getType().name())
                           .replace("%loaded%", String.valueOf(var5.isLoaded()))
                           .getLines()
                     )
                     .build()
               }
            );
      }

      this.page = var2;
   }

   private void quit() {
      InventoryClickEvent.getHandlerList().unregister(this.listener);
   }

   private class Listen implements Listener {
      private Listen() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent var1) {
         if (var1.getClickedInventory() != null
            && var1.getClickedInventory().getHolder() instanceof StandBrowser
            && var1.getCurrentItem() != null
            && var1.getCurrentItem().getType() != Material.AIR) {
            var1.setCancelled(true);
            Player var2 = (Player)var1.getWhoClicked();
            if (var1.getRawSlot() == StandBrowser.this.inv.getSize() - 6) {
               if (StandBrowser.this.page > 1) {
                  StandBrowser.this.inv.clear();
                  StandBrowser.this.openPage(var2, StandBrowser.this.page - 1);
                  StandBrowser.this.buildPane(var2);
               }
            } else if (var1.getRawSlot() == StandBrowser.this.inv.getSize() - 4) {
               if (StandBrowser.this.page < StandBrowser.this.allPages) {
                  StandBrowser.this.inv.clear();
                  StandBrowser.this.openPage(var2, StandBrowser.this.page + 1);
                  StandBrowser.this.buildPane(var2);
               }
            } else {
               Stand var3 = StandBrowser.this.stands.get(StandBrowser.this.page * 45 - 45 + var1.getRawSlot());
               if (var3 != null) {
                  if (!PermissionsCache.hasPermission(var2, "OnyxShop.editshop.shopstands.edit")) {
                     SendMessage.chatToPlayer(var2, Lang.NO_PERMISSIONS.get(var2));
                     return;
                  }

                  new StandManagement(StandBrowser.this.plugin, var2, StandBrowser.this.stands.get(StandBrowser.this.page * 45 - 45 + var1.getRawSlot()));
                  StandBrowser.this.quit();
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent var1) {
         if (var1.getInventory().getHolder() instanceof StandBrowser) {
            StandBrowser.this.quit();
         }
      }
   }
}
