package com.onyxshop.objects.inventorys;

import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class AuthRequest extends ShopInventory {
   private final Inventory inv;
   private final AuthRequest.Listen listener = new AuthRequest.Listen();
   private final OnyxShop plugin = OnyxShop.getInstance();
   private final Consumer<AuthRequest.AuthMethod> onComplete;

   public AuthRequest(Player player, Consumer<AuthRequest.AuthMethod> task) {
      this.inv = Bukkit.createInventory(this, 36, "§8§lAuthorization required");
      this.onComplete = task;
      this.buildPane();
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      player.openInventory(this.inv);
   }

   private void buildPane() {
      this.inv
         .setItem(
            12,
            new ItemBuilder(XMaterial.PAPER.parseItem())
               .withDisplayName("§a§lTemporary authorization")
               .withLore(
                  CalculateAmount.splitLongString(
                     "§cWhen using the temporary authorization method, the shop layout will be uploaded §c§lanomalously §r§cbut will only be available for 5 days only and removed after."
                  )
               )
               .build()
         );
      this.inv
         .setItem(
            14,
            new ItemBuilder(XMaterial.PAPER.parseItem())
               .withDisplayName("§9§lAuthorization via discord")
               .withLore(
                  CalculateAmount.splitLongString(
                     "§cWhen authorizing using discord, the shop layout will be linked to your discord account. \n §cThis allows you to update your existing layout and will be available until you remove the layout yourself."
                  )
               )
               .build()
         );
      this.inv.setItem(22, new ItemBuilder(XMaterial.NETHER_STAR.parseItem()).withDisplayName("§c§lCancel upload process").withEnchantGlint().build());
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

   public static enum AuthMethod {
      DISCORD,
      TEMP;

      private AuthMethod() {
      }
   }

   private class Listen implements Listener {
      private Listen() {
      }

      @EventHandler
      public void onInventoryClick(InventoryClickEvent event) {
         if (event.getClickedInventory() != null
            && event.getClickedInventory() == AuthRequest.this.inv
            && event.getCurrentItem() != null
            && event.getCurrentItem().getType() != Material.AIR) {
            event.setCancelled(true);
            if (event.getCurrentItem().getType() != AuthRequest.this.plugin.createItem.fillItem.getType()) {
               AuthRequest.AuthMethod authMethod = null;
               if (event.getRawSlot() == 12) {
                  authMethod = AuthRequest.AuthMethod.TEMP;
               } else if (event.getRawSlot() == 14) {
                  authMethod = AuthRequest.AuthMethod.DISCORD;
               }

               AuthRequest.this.quit();
               event.getWhoClicked().closeInventory();
               AuthRequest.this.onComplete.accept(authMethod);
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent event) {
         if (event.getInventory() == AuthRequest.this.inv) {
            AuthRequest.this.quit();
         }
      }
   }
}
