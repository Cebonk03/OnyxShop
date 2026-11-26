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

   public AvailableActions(Player var1, ShopItem var2, com.onyxshop.methodes.ItemEditor.Option var3) {
      this.item = var2;
      this.option = var3;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_AVAILABLE_ACTIONS, var1));
      List var2 = this.itemEditor.getAvailableActions(this.item.section, this.item.itemLoc, this.option.getKey());
      this.inv.setItem(1, (ItemStack)var2.get(0));
      this.inv.setItem(4, (ItemStack)var2.get(1));
      this.inv.setItem(7, (ItemStack)var2.get(2));
      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   public ShopItem getItem() {
      return this.item;
   }

   public com.onyxshop.methodes.ItemEditor.Option getOption() {
      return this.option;
   }
}
