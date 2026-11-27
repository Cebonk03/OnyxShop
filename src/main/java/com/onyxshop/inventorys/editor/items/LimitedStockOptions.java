package com.onyxshop.inventorys.editor.items;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LimitedStockOptions extends ItemEditor {
   private final ShopItem item;
   private final LimitedStockOptions.Type type;

   public LimitedStockOptions(Player player, ShopItem shopItem, LimitedStockOptions.Type type) {
      this.item = shopItem;
      this.type = type;
      this.initInventory(player);
      this.fillInventory();
      this.open(player);
   }

   private void initInventory(Player player) {
      this.inv = OnyxShop.getInstance()
         .getMetaUtils()
         .createInventory(
            this,
            18,
            TitleManager.get(
               this.type == LimitedStockOptions.Type.GLOBAL ? TitleManager.Menu.ITEM_EDITOR_GLOBAL_STOCK : TitleManager.Menu.ITEM_EDITOR_PLAYER_STOCK, player
            )
         );
      ConfigurationSection section = ConfigManager.getShop(this.item.section).getConfigurationSection("pages." + this.item.itemLoc);
      com.onyxshop.methodes.ItemEditor.Option option = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_LIMIT
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_LIMIT;
      com.onyxshop.methodes.ItemEditor.Option option2 = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_TIMER
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_TIMER;
      com.onyxshop.methodes.ItemEditor.Option option3 = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_FREQUENCY
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_FREQUENCY;
      ItemBuilder itemBuilder = new ItemBuilder((this.type == LimitedStockOptions.Type.GLOBAL ? XMaterial.ENDER_CHEST : XMaterial.CHEST).parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", option.getName()));
      if (section.contains(option.getKey())) {
         itemBuilder.withLore(Lang.CURRENT_VALUE.get().replace("%option%", option.getName()).getLegacy() + ChatColor.GREEN + section.getInt(option.getKey()));
      }

      this.inv.setItem(0, this.setEditOption(itemBuilder.build(), option));
      ItemBuilder itemBuilder2 = new ItemBuilder(XMaterial.CLOCK.parseItem());
      if (section.contains(option3.getKey())) {
         List<Translatable> multiple = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         multiple.forEach(entry -> entry.replace("%option%", option2.getName()).replace("%option2%", option3.getName()));
         itemBuilder2.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder2.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         this.inv.setItem(1, itemBuilder2.build());
      } else {
         itemBuilder2.setDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", option2.getName()));
         if (section.getString(option2.getKey()) != null) {
            itemBuilder2.withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", option2.getName()) + ChatColor.GREEN + section.getString(option2.getKey()));
         }

         this.inv.setItem(1, this.setEditOption(itemBuilder2.build(), option2));
      }

      ItemBuilder itemBuilder3 = new ItemBuilder(XMaterial.DAYLIGHT_DETECTOR.parseItem());
      if (section.contains(option2.getKey())) {
         List<Translatable> multiple = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         multiple.forEach(entry -> entry.replace("%option%", option3.getName()).replace("%option2%", option2.getName()));
         itemBuilder3.setCDisplayName((Translatable)multiple.get(0));
         itemBuilder3.setCLore(multiple.size() > 0 ? multiple.subList(1, multiple.size()) : multiple);
         this.inv.setItem(2, itemBuilder3.build());
      } else {
         itemBuilder3.setDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", option3.getName()));
         if (section.getString(option3.getKey()) != null) {
            itemBuilder3.withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", option3.getName()) + ChatColor.GREEN + section.getString(option3.getKey()));

            try {
               LinkedList shopIds = OnyxShop.getInstance().getScheduleManager().validate(section.getString(option3.getKey()));
               if (!shopIds.isEmpty()) {
                  itemBuilder3.addLore(" ");
                  itemBuilder3.addLore(ChatColor.GREEN + "Next schedule times(up to 5):");

                  for (int size = 0; size < shopIds.size(); size++) {
                     itemBuilder3.addLore(ChatColor.GRAY + "-" + ChatColor.GREEN + " " + TimeUtils.getDateTime(((Date)shopIds.get(size)).getTime()));
                  }
               }
            } catch (RuntimeException exception) {
            }
         }

         this.inv.setItem(2, this.setEditOption(itemBuilder3.build(), option3));
      }

      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setNBTString(ItemStack itemStack, String itemId, String text) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtString(itemStack, itemId, text);
   }

   private ItemStack setEditOption(ItemStack itemStack, com.onyxshop.methodes.ItemEditor.Option option) {
      return OnyxShop.getInstance().versionHandler.setEditOption(itemStack, option.getKey());
   }

   public ShopItem getItem() {
      return this.item;
   }

   public static enum Type {
      GLOBAL,
      PLAYER;

      private Type() {
      }
   }
}
