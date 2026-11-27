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
      Player player, ShopItem shopItem, com.onyxshop.methodes.ItemEditor.Option option, com.onyxshop.methodes.ItemEditor.Action action
   ) {
      this.item = shopItem;
      this.option = option;
      this.action = action;
      this.initInventory(player);
      this.fillInventory();
      this.open(player);
   }

   private void initInventory(Player player) {
      List stringList = ConfigManager.getShop(this.item.section).getStringList("pages." + this.item.itemLoc + "." + this.option.getKey());
      int slots = CalculateAmount.getSlots(stringList.size());
      this.inv = OnyxShop.getInstance().getMetaUtils().createInventory(this, slots, TitleManager.get(TitleManager.Menu.ITEM_EDITOR_CHOOSE_INDEX, player));
      switch (this.option) {
         case ENCHANTMENTS:
            for (int size = 0; size < stringList.size(); size++) {
               this.inv.addItem(new ItemStack[]{this.setIndex(this.itemEditor.getBook((String)stringList.get(size)), size)});
            }
            break;
         case POTIONTYPES:
            for (int index = 0; index < stringList.size(); index++) {
               this.inv
                  .addItem(
                     new ItemStack[]{this.setIndex(this.itemEditor.getPotion((String)stringList.get(index), this.item.section, "pages." + this.item.itemLoc), index)}
                  );
            }
            break;
         case RECIPES:
            for (int index = 0; index < stringList.size(); index++) {
               Optional optional = XMaterial.matchXMaterial((String)stringList.get(index));
               this.inv
                  .addItem(
                     new ItemStack[]{
                        this.setIndex(
                           optional.isPresent() && ((XMaterial)optional.get()).parseItem() != null
                              ? ((XMaterial)optional.get()).parseItem()
                              : new ItemBuilder(Material.BARRIER).withDisplayName((String)stringList.get(index)).build(),
                           index
                        )
                     }
                  );
            }
            break;
         case BUY_COMMANDS:
         case SELL_COMMANDS:
         case DISPLAYLORE:
         case LORE:
            for (int index = 0; index < stringList.size(); index++) {
               this.inv
                  .addItem(
                     new ItemStack[]{
                        this.setIndex(new ItemBuilder(Material.PAPER).withDisplayName(ChatUtil.formatColors((String)stringList.get(index))).build(), index)
                     }
                  );
            }
            break;
         case FIREWORK_COLORS:
         case FIREWORK_FADE_COLORS:
            List values = FireworkUtil.FireworkColor.matchColorsNames(stringList);

            for (int index = 0; index < values.size(); index++) {
               FireworkUtil.FireworkColor fireworkColor = (FireworkUtil.FireworkColor)values.get(index);
               ItemBuilder itemBuilder = new ItemBuilder(XMaterial.matchXMaterial("WOOL:" + fireworkColor.getId()).get().parseItem())
                  .withDisplayName(
                     OnyxShop.getInstance().version >= 116
                        ? ChatColor.of(fireworkColor.getColor()) + OnyxShop.getInstance().getMaterialName(fireworkColor.name())
                        : OnyxShop.getInstance().getMaterialName(fireworkColor.name())
                  );
               this.inv.addItem(new ItemStack[]{this.setIndex(itemBuilder.build(), index)});
            }
      }

      this.inv.setItem(slots - 9, OnyxShop.getInstance().createItem.getPlayerReminder(player, false));
      this.inv.setItem(slots - 1, new ItemBuilder(Material.BARRIER).withDisplayName(Lang.BACK.get()).build());
   }

   private ItemStack setIndex(ItemStack itemStack, int index) {
      return OnyxShop.getInstance().versionHandler.setOnyxNbtInt(itemStack, "Index", index);
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
