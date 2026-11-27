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
      Player player,
      ShopItem shopItem,
      com.onyxshop.methodes.ItemEditor.Option option,
      com.onyxshop.methodes.ItemEditor.Action action,
      String text
   ) {
      this.item = shopItem;
      this.newItem = text;
      this.option = option;
      this.action = action;
      this.initInventory(player);
      this.fillInventory();
      this.open(player);
      OnyxShop.getInstance()
         .runTaskAsync(
            () -> this.itemEditor
               .loadItemChanges(
                  this.action, this.option, this.action == com.onyxshop.methodes.ItemEditor.Action.ADD_ITEM ? text : this.item.getItemPath()
               )
         );
   }

   private void initInventory(Player player) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CONCLUSION, player));
      String text = null;
      switch (this.action) {
         case REMOVE_ITEM:
            text = Lang.ITEM_DELETED_FROM_SHOP.get().getLegacy();
            break;
         case REMOVE:
         case EDIT:
         case ADD:
            text = Lang.ITEM_EDITED.get().getLegacy();
            break;
         case ADD_ITEM:
            text = Lang.ITEM_CREATED.get().getLegacy();
      }

      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
      this.inv.setItem(4, new ItemBuilder(XMaterial.GREEN_TERRACOTTA.parseItem()).withDisplayName(Lang.STATUS.get()).withLore(text).build());
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
