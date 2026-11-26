package com.onyxshop.inventorys.editor.items;

import java.util.List;
import java.util.Optional;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.util.ChatUtil;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.ItemBuilder;
import com.onyxshop.util.XMaterial;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IndexChooser extends ItemEditor {
   private final ShopItem item;
   private final com.onyxshop.methodes.ItemEditor.Option option;
   private final com.onyxshop.methodes.ItemEditor.Action action;

   public IndexChooser(
      Player var1, ShopItem var2, com.onyxshop.methodes.ItemEditor.Option var3, com.onyxshop.methodes.ItemEditor.Action var4
   ) {
      this.item = var2;
      this.option = var3;
      this.action = var4;
      this.initInventory(var1);
      this.fillInventory();
      this.open(var1);
   }

   private void initInventory(Player var1) {
      List var2 = ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + "." + this.option.getKey());
      int var3 = CalculateAmount.getSlots(var2.size());
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, var3, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CHOOSE_INDEX, var1));
      switch (this.option) {
         case ENCHANTMENTS:
            for (int var11 = 0; var11 < var2.size(); var11++) {
               this.inv.addItem(new ItemStack[]{this.setIndex(this.itemEditor.getBook((String)var2.get(var11)), var11)});
            }
            break;
         case POTIONTYPES:
            for (int var10 = 0; var10 < var2.size(); var10++) {
               this.inv
                  .addItem(
                     new ItemStack[]{this.setIndex(this.itemEditor.getPotion((String)var2.get(var10), this.item.section, "pages." + this.item.itemLoc), var10)}
                  );
            }
            break;
         case RECIPES:
            for (int var9 = 0; var9 < var2.size(); var9++) {
               Optional var12 = XMaterial.matchXMaterial((String)var2.get(var9));
               this.inv
                  .addItem(
                     new ItemStack[]{
                        this.setIndex(
                           var12.isPresent() && ((XMaterial)var12.get()).parseItem() != null
                              ? ((XMaterial)var12.get()).parseItem()
                              : new ItemBuilder(Material.BARRIER).withDisplayName((String)var2.get(var9)).build(),
                           var9
                        )
                     }
                  );
            }
            break;
         case BUY_COMMANDS:
         case SELL_COMMANDS:
         case DISPLAYLORE:
         case LORE:
            for (int var8 = 0; var8 < var2.size(); var8++) {
               this.inv
                  .addItem(
                     new ItemStack[]{
                        this.setIndex(new ItemBuilder(Material.PAPER).withDisplayName(ChatUtil.formatColors((String)var2.get(var8))).build(), var8)
                     }
                  );
            }
            break;
         case FIREWORK_COLORS:
         case FIREWORK_FADE_COLORS:
            List var4 = FireworkUtil.FireworkColor.matchColorsNames(var2);

            for (int var5 = 0; var5 < var4.size(); var5++) {
               FireworkUtil.FireworkColor var6 = (FireworkUtil.FireworkColor)var4.get(var5);
               ItemBuilder var7 = new ItemBuilder(XMaterial.matchXMaterial("WOOL:" + var6.getId()).get().parseItem())
                  .withDisplayName(
                     OnyxShop.getInstance().version >= 116
                        ? ChatColor.of(var6.getColor()) + OnyxShop.getInstance().getMaterialName(var6.name())
                        : OnyxShop.getInstance().getMaterialName(var6.name())
                  );
               this.inv.addItem(new ItemStack[]{this.setIndex(var7.build(), var5)});
            }
      }

      this.inv.setItem(var3 - 9, OnyxShop.getInstance().createItem.getPlayerReminder(var1, false));
      this.inv.setItem(var3 - 1, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setIndex(ItemStack var1, int var2) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtInt(var1, "Index", var2);
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
}
