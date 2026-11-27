package com.onyxshop.objects.inventorys;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
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

public class StandManagement extends ShopInventory {
   private final Inventory inv;
   private final StandManagement.Listen listener = new StandManagement.Listen();
   private final OnyxShop plugin;
   private final Stand stand;

   public StandManagement(OnyxShop plugin, Player player, Stand stand) {
      this.inv = plugin.getMetaUtils().createInventory(this, 9, TitleManager.get(TitleManager.Menu.SHOPSTAND_MANAGEMENT, player));
      this.plugin = plugin;
      this.stand = stand;
      this.buildPane(player);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      player.openInventory(this.inv);
   }

   private void buildPane(Player player) {
      this.inv
         .setItem(
            0,
            new ItemBuilder(XMaterial.NETHER_STAR.parseItem())
               .withDisplayName(Lang.SHOP_STAND_INFO_ITEM_NAME.get(player))
               .withCLore(
                  Lang.SHOP_STAND_INFO_ITEM_LORE
                     .get(player)
                     .replace("%id%", String.valueOf(this.stand.getId()))
                     .replace("%location%", this.stand.getLoc().toString())
                     .replace("%item%", this.stand.getItem())
                     .replace("%type%", this.stand.getType().name())
                     .replace("%loaded%", String.valueOf(this.stand.isLoaded()))
                     .getLines()
               )
               .build()
         );
      if (ConfigManager.getConfig().getBoolean("shop-stands.holograms")) {
         this.inv
            .setItem(
               6,
               new ItemBuilder(Material.NAME_TAG)
                  .withDisplayName(Lang.TOGGLE_STAND_HOLOGRAM_ITEM_NAME.get(player))
                  .withCLore(
                     Lang.TOGGLE_STAND_HOLOGRAM_ITEM_LORE
                        .get(player)
                        .replace("%action%", this.stand.getSettings().isHologram() ? "disable" : "enable")
                        .getLines()
                  )
                  .build()
            );
      }

      this.inv
         .setItem(
            7,
            new ItemBuilder(Material.REDSTONE)
               .withDisplayName(Lang.FORCE_RELOAD_STAND_ITEM_NAME.get(player))
               .withCLore(Lang.FORCE_RELOAD_STAND_ITEM_LORE.get(player).getLines())
               .build()
         );
      this.inv
         .setItem(
            8,
            new ItemBuilder(XMaterial.BARRIER.parseItem())
               .withDisplayName(Lang.DESTROY_STAND_ITEM_NAME.get(player))
               .withCLore(Lang.DESTROY_STAND_ITEM_LORE.get(player).getLines())
               .build()
         );
      this.fillEmpty();
   }

   private void fillEmpty() {
      if (this.plugin.createItem.fillItem != null) {
         for (int size = 0; size < this.inv.getSize(); size++) {
            if (this.inv.getItem(size) == null) {
               this.inv.setItem(size, this.plugin.createItem.fillItem);
            }
         }
      }
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
            && event.getClickedInventory().getHolder() instanceof StandManagement
            && event.getCurrentItem() != null
            && event.getCurrentItem().getType() != Material.AIR) {
            event.setCancelled(true);
            Player player = (Player)event.getWhoClicked();
            if (event.getRawSlot() == 6 && ConfigManager.getConfig().getBoolean("shop-stands.holograms")) {
               StandManagement.this.stand.getSettings().toggleHologram();
               StandManagement.this.plugin.getStandProvider().reloadStand(StandManagement.this.stand);
               StandManagement.this.inv
                  .setItem(
                     6,
                     new ItemBuilder(Material.NAME_TAG)
                        .withDisplayName(Lang.TOGGLE_STAND_HOLOGRAM_ITEM_NAME.get(player))
                        .withCLore(
                           Lang.TOGGLE_STAND_HOLOGRAM_ITEM_LORE
                              .get(player)
                              .replace("%action%", StandManagement.this.stand.getSettings().isHologram() ? "disable" : "enable")
                              .getLines()
                        )
                        .build()
                  );
            } else if (event.getRawSlot() == 7) {
               if (StandManagement.this.plugin.getStandProvider().reloadStand(StandManagement.this.stand)) {
                  SendMessage.chatToPlayer(player, Lang.RELOADED_SHOP_STAND.get(player).replace("%id%", String.valueOf(StandManagement.this.stand.getId())));
               }
            } else {
               if (event.getRawSlot() != 8) {
                  return;
               }

               if (!PermissionsCache.hasPermission(player, "OnyxShop.editshop.shopstands.destroy")) {
                  SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS.get(player));
                  return;
               }

               StandManagement.this.plugin.getStandProvider().destroy(StandManagement.this.stand);
               SendMessage.chatToPlayer(player, Lang.REMOVED_SHOP_STAND.get(player).replace("%location%", StandManagement.this.stand.getLoc().toString()));
            }

            StandManagement.this.quit();
            event.getWhoClicked().closeInventory();
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (event.getInventory().getHolder() instanceof StandManagement) {
            StandManagement.this.quit();
         }
      }
   }
}
