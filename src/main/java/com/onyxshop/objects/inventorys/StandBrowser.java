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

   public StandBrowser(OnyxShop plugin, Player player) {
      this.stands = plugin.getStandProvider().getStands().stream().sorted(Comparator.comparingInt(Stand::getId)).collect(Collectors.toCollection(ArrayList::new));
      int size = this.stands.size();
      this.allPages = (int)Math.ceil(size / 45.0);
      this.inv = plugin.getMetaUtils().createInventory(this, CalculateAmount.getSlots(size), TitleManager.get(TitleManager.Menu.SHOPSTAND_BROWSER, player));
      this.plugin = plugin;
      this.buildPane(player);
      this.openPage(player, 1);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      player.openInventory(this.inv);
   }

   private void buildPane(Player player) {
      this.inv
         .setItem(
            this.inv.getSize() - 5,
            new ItemBuilder(XMaterial.COMPASS.parseItem())
               .withDisplayName(
                  Lang.CURRENT_PAGE.get(player).builder().append(" ").append(String.valueOf(this.page)).append("/").append(String.valueOf(this.allPages))
                     + " "
                     + this.page
                     + "/"
                     + this.allPages
               )
               .build()
         );
      this.inv
         .setItem(this.inv.getSize() - 6, new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.PREVIOUS_PAGE.get(player)).build());
      this.inv
         .setItem(
            this.inv.getSize() - 4, new ItemBuilder(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem()).withDisplayName(Lang.NEXT_PAGE.get(player)).build()
         );
   }

   private void openPage(Player player, int page) {
      int value = page * 45 - 45;

      for (int size = value; size < value + Math.min(this.stands.size() - value, 45); size++) {
         Stand stand = this.stands.get(size);
         this.inv
            .addItem(
               new ItemStack[]{
                  new ItemBuilder(XMaterial.NETHER_STAR.parseItem())
                     .withDisplayName(Lang.SHOP_STAND_INFO_ITEM_NAME.get(player))
                     .withCLore(
                        Lang.SHOP_STAND_INFO_ITEM_LORE
                           .get(player)
                           .replace("%id%", String.valueOf(stand.getId()))
                           .replace("%location%", stand.getLoc().toString())
                           .replace("%item%", stand.getItem())
                           .replace("%type%", stand.getType().name())
                           .replace("%loaded%", String.valueOf(stand.isLoaded()))
                           .getLines()
                     )
                     .build()
               }
            );
      }

      this.page = page;
   }

   private void quit() {
      InventoryClickEvent.getHandlerList().unregister(this.listener);
   }

   private class Listen implements Listener {
      private Listen() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent event) {
         if (event.getClickedInventory() != null
            && event.getClickedInventory().getHolder() instanceof StandBrowser
            && event.getCurrentItem() != null
            && event.getCurrentItem().getType() != Material.AIR) {
            event.setCancelled(true);
            Player player = (Player)event.getWhoClicked();
            if (event.getRawSlot() == StandBrowser.this.inv.getSize() - 6) {
               if (StandBrowser.this.page > 1) {
                  StandBrowser.this.inv.clear();
                  StandBrowser.this.openPage(player, StandBrowser.this.page - 1);
                  StandBrowser.this.buildPane(player);
               }
            } else if (event.getRawSlot() == StandBrowser.this.inv.getSize() - 4) {
               if (StandBrowser.this.page < StandBrowser.this.allPages) {
                  StandBrowser.this.inv.clear();
                  StandBrowser.this.openPage(player, StandBrowser.this.page + 1);
                  StandBrowser.this.buildPane(player);
               }
            } else {
               Stand stand = StandBrowser.this.stands.get(StandBrowser.this.page * 45 - 45 + event.getRawSlot());
               if (stand != null) {
                  if (!PermissionsCache.hasPermission(player, "OnyxShop.editshop.shopstands.edit")) {
                     SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                     return;
                  }

                  new StandManagement(StandBrowser.this.plugin, player, StandBrowser.this.stands.get(StandBrowser.this.page * 45 - 45 + event.getRawSlot()));
                  StandBrowser.this.quit();
               }
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (event.getInventory().getHolder() instanceof StandBrowser) {
            StandBrowser.this.quit();
         }
      }
   }
}
