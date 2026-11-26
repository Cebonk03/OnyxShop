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

   public void open(Player var1) {
      var1.openInventory(this.inv);
   }

   public void fillInventory() {
      if (OnyxShop.getInstance().createItem.fillItem != null) {
         for (int var1 = 0; var1 < this.inv.getSize() - 9; var1++) {
            if (this.inv.getItem(var1) == null) {
               this.inv.setItem(var1, OnyxShop.getInstance().createItem.fillItem);
            }
         }
      }
   }
}
