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

public class ConclusionScreen extends ItemEditor {
   private final ShopItem item;
   private final String newItem;
   private final com.onyxshop.methodes.ItemEditor.Option option;
   private final com.onyxshop.methodes.ItemEditor.Action action;

   public ConclusionScreen(
      Player var1,
      ShopItem var2,
      com.onyxshop.methodes.ItemEditor.Option var3,
      com.onyxshop.methodes.ItemEditor.Action var4,
      String var5
   ) {
      this.item = var2;
      this.newItem = var5;
      this.option = var3;
      this.action = var4;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
      OnyxShop.getInstance()
         .runTaskAsync(
            () -> this.itemEditor
               .loadItemChanges(
                  this.action, this.option, this.action == com.onyxshop.methodes.ItemEditor.Action.ADD_ITEM ? var5 : this.item.getItemPath()
               )
         );
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CONCLUSION, var1));
      String var2 = null;
      switch (this.action) {
         case REMOVE_ITEM:
            var2 = Lang.ITEM_DELETED_FROM_SHOP.get().getLegacy();
            break;
         case REMOVE:
         case EDIT:
         case ADD:
            var2 = Lang.ITEM_EDITED.get().getLegacy();
            break;
         case ADD_ITEM:
            var2 = Lang.ITEM_CREATED.get().getLegacy();
      }

      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(4, new ItemBuilder(XMaterial.GREEN_TERRACOTTA.parseItem()).withDisplayName(Lang.STATUS.get()).withLore(var2).build());
   }

   public ShopItem getItem() {
      return this.item;
   }

   public com.onyxshop.methodes.ItemEditor.Option getOption() {
      return this.option;
   }

   public com.onyxshop.methodes.ItemEditor.Action getAction() {
      return this.action;
   }

   public String getNewItem() {
      return this.newItem;
   }
}
