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

   public StandManagement(OnyxShop var1, Player var2, Stand var3) {
      this.inv = var1.getMetaUtils().createInventory(this, 9, TitleManager.get(TitleManager.Menu.SHOPSTAND_MANAGEMENT, var2));
      this.plugin = var1;
      this.stand = var3;
      this.buildPane(var2);
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      var2.openInventory(this.inv);
   }

   private void buildPane(Player var1) {
      this.inv
         .setItem(
            0,
            new ItemBuilder(XMaterial.NETHER_STAR.parseItem())
               .withDisplayName(Lang.SHOP_STAND_INFO_ITEM_NAME.get(var1))
               .withCLore(
                  Lang.SHOP_STAND_INFO_ITEM_LORE
                     .get(var1)
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
                  .withDisplayName(Lang.TOGGLE_STAND_HOLOGRAM_ITEM_NAME.get(var1))
                  .withCLore(
                     Lang.TOGGLE_STAND_HOLOGRAM_ITEM_LORE
                        .get(var1)
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
               .withDisplayName(Lang.FORCE_RELOAD_STAND_ITEM_NAME.get(var1))
               .withCLore(Lang.FORCE_RELOAD_STAND_ITEM_LORE.get(var1).getLines())
               .build()
         );
      this.inv
         .setItem(
            8,
            new ItemBuilder(XMaterial.BARRIER.parseItem())
               .withDisplayName(Lang.DESTROY_STAND_ITEM_NAME.get(var1))
               .withCLore(Lang.DESTROY_STAND_ITEM_LORE.get(var1).getLines())
               .build()
         );
      this.fillEmpty();
   }

   private void fillEmpty() {
      if (this.plugin.createItem.fillItem != null) {
         for (int var1 = 0; var1 < this.inv.getSize(); var1++) {
            if (this.inv.getItem(var1) == null) {
               this.inv.setItem(var1, this.plugin.createItem.fillItem);
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
      public void onInventoryClick(InventoryClickEvent var1) {
         if (var1.getClickedInventory() != null
            && var1.getClickedInventory().getHolder() instanceof StandManagement
            && var1.getCurrentItem() != null
            && var1.getCurrentItem().getType() != Material.AIR) {
            var1.setCancelled(true);
            Player var2 = (Player)var1.getWhoClicked();
            if (var1.getRawSlot() == 6 && ConfigManager.getConfig().getBoolean("shop-stands.holograms")) {
               StandManagement.this.stand.getSettings().toggleHologram();
               StandManagement.this.plugin.getStandProvider().reloadStand(StandManagement.this.stand);
               StandManagement.this.inv
                  .setItem(
                     6,
                     new ItemBuilder(Material.NAME_TAG)
                        .withDisplayName(Lang.TOGGLE_STAND_HOLOGRAM_ITEM_NAME.get(var2))
                        .withCLore(
                           Lang.TOGGLE_STAND_HOLOGRAM_ITEM_LORE
                              .get(var2)
                              .replace("%action%", StandManagement.this.stand.getSettings().isHologram() ? "disable" : "enable")
                              .getLines()
                        )
                        .build()
                  );
            } else if (var1.getRawSlot() == 7) {
               if (StandManagement.this.plugin.getStandProvider().reloadStand(StandManagement.this.stand)) {
                  SendMessage.chatToPlayer(var2, Lang.RELOADED_SHOP_STAND.get(var2).replace("%id%", String.valueOf(StandManagement.this.stand.getId())));
               }
            } else {
               if (var1.getRawSlot() != 8) {
                  return;
               }

               if (!PermissionsCache.hasPermission(var2, "OnyxShop.editshop.shopstands.destroy")) {
                  SendMessage.chatToPlayer(var2, Lang.NO_PERMISSIONS.get(var2));
                  return;
               }

               StandManagement.this.plugin.getStandProvider().destroy(StandManagement.this.stand);
               SendMessage.chatToPlayer(var2, Lang.REMOVED_SHOP_STAND.get(var2).replace("%location%", StandManagement.this.stand.getLoc().toString()));
            }

            StandManagement.this.quit();
            var1.getWhoClicked().closeInventory();
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent var1) {
         if (var1.getInventory().getHolder() instanceof StandManagement) {
            StandManagement.this.quit();
         }
      }
   }
}
