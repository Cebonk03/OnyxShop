package com.onyxshop.inventorys.editor.items;

import java.util.Collections;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DynamicPricingOptions extends ItemEditor {
   private final ShopItem item;

   public DynamicPricingOptions(Player var1, ShopItem var2) {
      this.item = var2;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, 18, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_DYNAMIC_PRICING, var1));
      int var2 = ConfigManager.getShop(this.item.section)
         .getInt(
            "pages." + this.item.itemLoc + "." + com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_MAX_STOCK.getKey(),
            OnyxShop.getInstance().getDynamicPricingProvider().getMaxStock(this.item.getItemPath())
         );
      ItemBuilder var3 = new ItemBuilder(XMaterial.CHEST.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DYNAMIC_PRICING_MAX_STOCK.get()))
         .withLore(Lang.CURRENT_VALUE.get().replace("%option%", Lang.OPTION_DYNAMIC_PRICING_MAX_STOCK.get()).builder().append(String.valueOf(var2)).build())
         .addLore(
            CalculateAmount.splitLongString(
               ChatColor.WHITE
                  + "Lower values mean faster dynamic price changes, higher values will require the item to be purchased/sold more for price changes to occur."
            )
         );
      this.inv
         .setItem(
            1,
            this.setEditOption(
               this.setNBTString(var3.build(), "value", String.valueOf(var2)), com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_MAX_STOCK
            )
         );
      boolean var4 = ConfigManager.getShop(this.item.section)
         .getBoolean(
            "pages." + this.item.itemLoc + "." + com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_ENABLE.getKey(),
            this.item.isDynamicPricing()
         );
      ItemBuilder var5 = new ItemBuilder(XMaterial.LEVER.parseItem())
         .withDisplayName(Lang.EDIT_OPTION_OF_ITEM.get().replace("%option%", Lang.OPTION_DYNAMIC_PRICING_ENABLE.get()))
         .withCLore(
            Collections.singletonList(
               Lang.CURRENT_VALUE
                  .get()
                  .replace("%option%", Lang.OPTION_DYNAMIC_PRICING_ENABLE.get())
                  .builder()
                  .color((char)(var4 ? 'a' : 'c'))
                  .append(var4 ? "true" : "false")
                  .build()
            )
         );
      if (var4) {
         var5.withEnchantGlint();
      }

      this.inv
         .setItem(
            0,
            this.setEditOption(
               this.setNBTString(var5.build(), "value", String.valueOf(var4)), com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_ENABLE
            )
         );
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
}
