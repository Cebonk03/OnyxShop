package com.onyxshop.inventorys.editor.items;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Main extends ItemEditor {
   private final ShopItem item;

   public Main(Player var1, ShopItem var2) {
      this.item = var2;
      if (OnyxShop.getInstance().useItemEditor) {
         UserManager.getUser(var1).setOpenNewGUI(true);
         this.initInventory(var1);
         this.fillInventory();
         this.open(var1);
      }
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 54, TitleManager.get(TitleManager.Menu.ITEM_EDITOR, var1));
      this.inv.setItem(45, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(53, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(13, this.itemEditor.getSelectedItem(this.item));
      this.inv.setItem(29, new ItemBuilder(XMaterial.LIME_STAINED_GLASS.parseItem()).withDisplayName(Lang.CREATE_NEW_ITEM_IN_SHOP.get()).build());
      this.inv.setItem(31, new ItemBuilder(XMaterial.BLUE_STAINED_GLASS.parseItem()).withDisplayName(Lang.EDIT_ITEM.get()).build());
      this.inv.setItem(33, new ItemBuilder(XMaterial.RED_STAINED_GLASS.parseItem()).withDisplayName(Lang.DELETE_ITEM_IN_SHOP.get()).build());
   }

   public ShopItem getItem() {
      return this.item;
   }
}
