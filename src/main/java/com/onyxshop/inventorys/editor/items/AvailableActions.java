package com.onyxshop.inventorys.editor.items;

import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AvailableActions extends ItemEditor {
   private final ShopItem item;
   private final com.onyxshop.methodes.ItemEditor.Option option;

   public AvailableActions(Player player, ShopItem shopItem, com.onyxshop.methodes.ItemEditor.Option option) {
      this.item = shopItem;
      this.option = option;
      this.initInventory(player);
      this.fillInventory();
      this.open(player);
   }

   private void initInventory(Player player) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_AVAILABLE_ACTIONS, player));
      List availableActions = this.itemEditor.getAvailableActions(this.item.section, this.item.itemLoc, this.option.getKey());
      this.inv.setItem(1, (ItemStack)availableActions.get(0));
      this.inv.setItem(4, (ItemStack)availableActions.get(1));
      this.inv.setItem(7, (ItemStack)availableActions.get(2));
      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   public ShopItem getItem() {
      return this.item;
   }

   public com.onyxshop.methodes.ItemEditor.Option getOption() {
      return this.option;
   }
}
