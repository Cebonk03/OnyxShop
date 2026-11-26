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

   public LimitedStockOptions(Player var1, ShopItem var2, LimitedStockOptions.Type var3) {
      this.item = var2;
      this.type = var3;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance()
         .getMetaUtils()
         .createInventory(
            this,
            18,
            TitleManager.get(
               this.type == LimitedStockOptions.Type.GLOBAL ? TitleManager.Menu.ITEM_EDITOR_GLOBAL_STOCK : TitleManager.Menu.ITEM_EDITOR_PLAYER_STOCK, var1
            )
         );
      ConfigurationSection var2 = ConfigManager.getShop(this.item.section).getConfigurationSection("pages." + this.item.itemLoc);
      com.onyxshop.methodes.ItemEditor.Option var3 = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_LIMIT
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_LIMIT;
      com.onyxshop.methodes.ItemEditor.Option var4 = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_TIMER
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_TIMER;
      com.onyxshop.methodes.ItemEditor.Option var5 = this.type == LimitedStockOptions.Type.GLOBAL
         ? com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK_FREQUENCY
         : com.onyxshop.methodes.ItemEditor.Option.PLAYER_STOCK_FREQUENCY;
      ItemBuilder var6 = new ItemBuilder((this.type == LimitedStockOptions.Type.GLOBAL ? XMaterial.ENDER_CHEST : XMaterial.CHEST).parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", var3.getName()));
      if (var2.contains(var3.getKey())) {
         var6.withLore(Lang.CURRENT_VALUE.get().replace("%option%", var3.getName()).getLegacy() + ChatColor.GREEN + var2.getInt(var3.getKey()));
      }

      this.inv.setItem(0, this.setEditOption(var6.build(), var3));
      ItemBuilder var7 = new ItemBuilder(XMaterial.CLOCK.parseItem());
      if (var2.contains(var5.getKey())) {
         List<Translatable> var8 = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         var8.forEach(var2x -> var2x.replace("%option%", var4.getName()).replace("%option2%", var5.getName()));
         var7.setCDisplayName((Translatable)var8.get(0));
         var7.setCLore(var8.size() > 0 ? var8.subList(1, var8.size()) : var8);
         this.inv.setItem(1, var7.build());
      } else {
         var7.setDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", var4.getName()));
         if (var2.getString(var4.getKey()) != null) {
            var7.withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", var4.getName()) + ChatColor.GREEN + var2.getString(var4.getKey()));
         }

         this.inv.setItem(1, this.setEditOption(var7.build(), var4));
      }

      ItemBuilder var12 = new ItemBuilder(XMaterial.DAYLIGHT_DETECTOR.parseItem());
      if (var2.contains(var4.getKey())) {
         List<Translatable> var9 = Lang.DISABLED_OPTION_CONFLICT.getMultiple();
         var9.forEach(var2x -> var2x.replace("%option%", var5.getName()).replace("%option2%", var4.getName()));
         var12.setCDisplayName((Translatable)var9.get(0));
         var12.setCLore(var9.size() > 0 ? var9.subList(1, var9.size()) : var9);
         this.inv.setItem(2, var12.build());
      } else {
         var12.setDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", var5.getName()));
         if (var2.getString(var5.getKey()) != null) {
            var12.withLore(Lang.CURRENT_VALUE.get().getLegacy().replace("%option%", var5.getName()) + ChatColor.GREEN + var2.getString(var5.getKey()));

            try {
               LinkedList var13 = OnyxShop.getInstance().getScheduleManager().validate(var2.getString(var5.getKey()));
               if (!var13.isEmpty()) {
                  var12.addLore(" ");
                  var12.addLore(ChatColor.GREEN + "Next schedule times(up to 5):");

                  for (int var10 = 0; var10 < var13.size(); var10++) {
                     var12.addLore(ChatColor.GRAY + "-" + ChatColor.GREEN + " " + TimeUtils.getDateTime(((Date)var13.get(var10)).getTime()));
                  }
               }
            } catch (RuntimeException var11) {
            }
         }

         this.inv.setItem(2, this.setEditOption(var12.build(), var5));
      }

      this.inv.setItem(9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(17, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setNBTString(ItemStack var1, String var2, String var3) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtString(var1, var2, var3);
   }

   private ItemStack setEditOption(ItemStack var1, com.onyxshop.methodes.ItemEditor.Option var2) {
      return OnyxShop.getInstance().versionHandler.setEditOption(var1, var2.getKey());
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
