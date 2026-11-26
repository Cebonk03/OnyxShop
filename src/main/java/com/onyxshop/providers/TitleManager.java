package com.onyxshop.providers;

import java.util.HashMap;
import java.util.Map;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.objects.titles.GUITitle;
import org.bukkit.entity.Player;

public class TitleManager {
   private static final Map<TitleManager.Menu, GUITitle> menuTitles = new HashMap<>();

   public TitleManager() {
   }

   public void load() {
      for (TitleManager.Menu var4 : TitleManager.Menu.values()) {
         menuTitles.put(var4, new GUITitle(var4.title, var4.title.getKey() + "-bedrock"));
      }
   }

   public static Translatable get(TitleManager.Menu var0, Player var1) {
      return menuTitles.get(var0).getTitle(var1);
   }

   public static enum Menu {
      MAIN_MENU(Lang.INVENTORY_MAIN_SHOP_TITLE),
      SELL_GUI(Lang.INVENTORY_SELLGUI_TITLE),
      BUY_SCREEN(Lang.INVENTORY_HOWMUCHBUY_TITLE),
      SELL_SCREEN(Lang.INVENTORY_HOWMUCHSELL_TITLE),
      BUY_STACKS(Lang.INVENTORY_BUYSTACKS_TITLE),
      SHOPSTAND_BUY_SCREEN(Lang.SHOPSTAND_BUY_SCREEN_TITLE),
      SHOPSTAND_SELL_SCREEN(Lang.SHOPSTAND_SELL_SCREEN_TITLE),
      SHOPSTAND_MANAGEMENT(Lang.SHOP_STANDS_MANAGEMENT_INVENTORY_TITLE),
      SHOPSTAND_BROWSER(Lang.SHOP_STAND_BROWSE_INVENTORY_TITLE),
      SHOPSEARCH(Lang.SHOPSEARCH_TITLE),
      SHOPSEARCH_RESULT(Lang.SHOPSEARCH_RESULT_TITLE),
      ITEM_EDITOR(Lang.ITEM_EDITOR_TITLE),
      ITEM_EDITOR_DELETE_ITEM_CONFIRMATION(Lang.ITEM_EDITOR_DELETE_ITEM_CONFIRMATION_TITLE),
      ITEM_EDITOR_CONCLUSION(Lang.ITEM_EDITOR_CONCLUSION_TITLE),
      ITEM_EDITOR_AVAILABLE_OPTIONS(Lang.ITEM_EDITOR_AVAILABLE_OPTIONS_TITLE),
      ITEM_EDITOR_AVAILABLE_ACTIONS(Lang.ITEM_EDITOR_AVAILABLE_ACTIONS_TITLE),
      ITEM_EDITOR_CHOOSE_VALUE(Lang.ITEM_EDITOR_CHOOSE_VALUE_TITLE),
      ITEM_EDITOR_CHOOSE_INDEX(Lang.ITEM_EDITOR_CHOOSE_INDEX_TITLE),
      ITEM_EDITOR_FIREWORK(Lang.ITEM_EDITOR_FIREWORK_TITLE),
      ITEM_EDITOR_DYNAMIC_PRICING(Lang.ITEM_EDITOR_DYNAMIC_PRICING_TITLE),
      ITEM_EDITOR_GLOBAL_STOCK(Lang.ITEM_EDITOR_GLOBAL_STOCK_TITLE),
      ITEM_EDITOR_PLAYER_STOCK(Lang.ITEM_EDITOR_PLAYER_STOCK_TITLE);

      private final Lang title;

      private Menu(Lang nullxx) {
         this.title = nullxx;
      }
   }
}
