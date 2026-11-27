package com.onyxshop.objects;

import java.util.LinkedList;
import java.util.function.Consumer;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CalculateAmount;
import com.onyxshop.providers.TitleManager;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.QueryBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopSearch extends ShopInventory {
   private Player player;
   private Inventory inv;
   private LinkedList<ShopItem> results;
   private int page;
   private int pages;
   private String search;
   private ShopSearch.QueryMethod currentQuery = OnyxShop.getInstance().defaultSearchMode;
   private static final int size;

   public int getPage() {
      return this.page;
   }

   public int getPages() {
      return this.pages;
   }

   public int getSize() {
      return this.results.size();
   }

   public ShopSearch(Player player, String query) {
      this.player = player;
      this.search = query;
      this.getResults(this::init, this.currentQuery, this.search);
   }

   private void init(LinkedList<ShopItem> shopItems) {
      OnyxShop.getInstance()
         .runTaskFor(
            () -> {
               this.results = shopItems;
               this.inv = OnyxShop.getInstance()
                  .getMetaUtils()
                  .createInventory(
                     this,
                     size == -1 ? CalculateAmount.getInvSlots(UserManager.getUser(this.player), this.results.size()) : size,
                     TitleManager.get(TitleManager.Menu.SHOPSEARCH_RESULT, this.player).replace("%search-query%", this.search)
                  );
               this.open(1);
            },
            this.player
         );
   }

   public void open(int value) {
      if (OnyxShop.getInstance().shopSearch) {
         if (!this.inv.isEmpty()) {
            this.inv.clear();
         }

         this.page = value;
         int size2 = (size == -1 ? 54 : size) - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0);
         int value2 = value * size2 - size2;

         for (int index = 0; index < Math.min(this.results.size() - value2, 45); index++) {
            ShopItem shopItem = this.results.get(value2 + index);
            this.inv.setItem(index, OnyxShop.getInstance().getSection(shopItem.section).updateItem(this.player, shopItem));
         }

         OnyxShop.getInstance().navBar.addSearchNavBar(this.inv, this.player, this.currentQuery.cycle(), value, this.pages);
         this.open(this.player, UserManager.getUser(this.player));
      }
   }

   private void open(Player player, User user) {
      user.setOpenNewGUI(true);
      player.openInventory(this.inv);
   }

   public void cycleQueryMethod() {
      this.currentQuery = this.currentQuery.cycle();
   }

   public void newSearch() {
      new AnvilGUI.Builder()
         .onComplete((player, input) -> {
            this.search = input;
            this.getResults(this::init, this.currentQuery, this.search);
            return AnvilGUI.Response.close();
         })
         .onClose(player -> OnyxShop.getInstance().runTaskLaterFor(this.player::updateInventory, this.player, 1L))
         .itemLeft(new ItemStack(Material.PAPER))
         .text(this.search)
         .title(Lang.SHOPSEARCH_TITLE.get(this.player).getLegacy())
         .plugin(OnyxShop.getInstance())
         .open(this.player);
   }

   public ShopItem getShopItemForSlot(int slot) {
      int size2 = (size == -1 ? 54 : size) - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0);
      int offset = this.page * size2 - size2;
      return this.results.get(offset + slot);
   }

   private void getResults(Consumer<LinkedList<ShopItem>> task, ShopSearch.QueryMethod queryMethod, String shopId) {
      if (OnyxShop.getInstance().shopSearch) {
         OnyxShop.getInstance().runTaskAsync(() -> {
            LinkedList values = null;
            QueryBuilder queryBuilder = OnyxShop.getInstance().getQueryBuilder();
            switch (queryMethod) {
               case BY_NAME:
                  values = queryBuilder.queryByName(shopId);
                  break;
               case BY_TYPE:
                  values = queryBuilder.queryByType(shopId);
            }

            int size2 = size == -1 ? CalculateAmount.getInvSlots(UserManager.getUser(this.player), values.size()) : size;
            this.pages = (int)Math.ceil(values.size() / ((double)size2 - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0)));
            task.accept(values);
         });
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   static {
      int value = Math.min(ConfigManager.getConfig().getInt("shop-search-screen.gui-rows", -1), 6);
      size = value < 1 ? -1 : value * 9;
   }

   public static enum QueryMethod {
      BY_NAME,
      BY_TYPE;

      private QueryMethod() {
      }

      public String getName() {
         String name = "SHOPSEARCH_METHOD_" + this.name();
         return Lang.valueOf(name).get().getLegacy();
      }

      public ShopSearch.QueryMethod cycle() {
         switch (this) {
            case BY_NAME:
               return BY_TYPE;
            case BY_TYPE:
               return BY_NAME;
            default:
               return BY_NAME;
         }
      }
   }
}
