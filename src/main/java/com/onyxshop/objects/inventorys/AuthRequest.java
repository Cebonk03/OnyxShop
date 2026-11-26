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

   public AuthRequest(Player var1, Consumer<AuthRequest.AuthMethod> var2) {
      this.inv = Bukkit.createInventory(this, 36, "§8§lAuthorization required");
      this.onComplete = var2;
      this.buildPane();
      Bukkit.getPluginManager().registerEvents(this.listener, this.plugin);
      var1.openInventory(this.inv);
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
      public void onInventoryClick(InventoryClickEvent var1) {
         if (var1.getClickedInventory() != null
            && var1.getClickedInventory() == AuthRequest.this.inv
            && var1.getCurrentItem() != null
            && var1.getCurrentItem().getType() != Material.AIR) {
            var1.setCancelled(true);
            if (var1.getCurrentItem().getType() != AuthRequest.this.plugin.createItem.fillItem.getType()) {
               AuthRequest.AuthMethod var2 = null;
               if (var1.getRawSlot() == 12) {
                  var2 = AuthRequest.AuthMethod.TEMP;
               } else if (var1.getRawSlot() == 14) {
                  var2 = AuthRequest.AuthMethod.DISCORD;
               }

               AuthRequest.this.quit();
               var1.getWhoClicked().closeInventory();
               AuthRequest.this.onComplete.accept(var2);
            }
         }
      }

      @EventHandler
      public void onInventoryClose(InventoryCloseEvent var1) {
         if (var1.getInventory() == AuthRequest.this.inv) {
            AuthRequest.this.quit();
         }
      }
   }
}
