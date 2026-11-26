package com.onyxshop.inventorys.editor.items;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DeleteItemConfirmation extends ItemEditor {
   private final ShopItem item;

   public DeleteItemConfirmation(Player var1, ShopItem var2) {
      this.item = var2;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance()
         .getMetaUtils()
         .createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_DELETE_ITEM_CONFIRMATION, var1));
      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(3, new ItemBuilder(XMaterial.GREEN_TERRACOTTA.parseItem()).withDisplayName(Lang.CONFIRM_DELETE_ITEM.get()).build());
      this.inv.setItem(5, new ItemBuilder(XMaterial.RED_TERRACOTTA.parseItem()).withDisplayName(Lang.CANCEL_DELETE_ITEM.get()).build());
   }

   public ShopItem getItem() {
      return this.item;
   }
}
