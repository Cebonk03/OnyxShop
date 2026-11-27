package com.onyxshop.inventorys.editor;

import com.onyxshop.OnyxShop;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ItemEditor implements InventoryHolder {
   public com.onyxshop.methodes.ItemEditor itemEditor;
   public Inventory inv;

   public ItemEditor() {
      this.itemEditor = OnyxShop.getInstance().itemEditor;
   }

   public Inventory getInventory() {
      return this.inv;
   }

   public void open(Player player) {
      player.openInventory(this.inv);
   }

   public void fillInventory() {
      if (OnyxShop.getInstance().createItem.fillItem != null) {
         for (int size = 0; size < this.inv.getSize() - 9; size++) {
            if (this.inv.getItem(size) == null) {
               this.inv.setItem(size, OnyxShop.getInstance().createItem.fillItem);
            }
         }
      }
   }
}
