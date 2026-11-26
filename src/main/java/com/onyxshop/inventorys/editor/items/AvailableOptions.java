package com.onyxshop.inventorys.editor.items;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AvailableOptions extends ItemEditor {
   private final ShopItem item;

   public AvailableOptions(Player var1, ShopItem var2) {
      this.item = var2;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 54, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_AVAILABLE_OPTIONS, var1));

      for (ItemStack var3 : this.itemEditor.getAllKeys(this.item.section, this.item.itemLoc)) {
         this.inv.addItem(new ItemStack[]{var3});
      }

      this.inv.setItem(45, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(53, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   public ShopItem getItem() {
      return this.item;
   }
}
