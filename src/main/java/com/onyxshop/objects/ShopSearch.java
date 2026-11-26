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

   public ShopSearch(Player var1, String var2) {
      this.player = var1;
      this.search = var2;
      this.getResults(this::init, this.currentQuery, this.search);
   }

   private void init(LinkedList<ShopItem> var1) {
      OnyxShop.getInstance()
         .runTask(
            () -> {
               this.results = var1;
               this.inv = OnyxShop.getInstance()
                  .getMetaUtils()
                  .createInventory(
                     this,
                     size == -1 ? CalculateAmount.getInvSlots(UserManager.getUser(this.player), this.results.size()) : size,
                     TitleManager.get(TitleManager.Menu.SHOPSEARCH_RESULT, this.player).replace("%search-query%", this.search)
                  );
               this.open(1);
            }
         );
   }

   public void open(int var1) {
      if (OnyxShop.getInstance().shopSearch) {
         if (!this.inv.isEmpty()) {
            this.inv.clear();
         }

         this.page = var1;
         int var2 = (size == -1 ? 54 : size) - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0);
         int var3 = var1 * var2 - var2;

         for (int var4 = 0; var4 < Math.min(this.results.size() - var3, 45); var4++) {
            ShopItem var5 = this.results.get(var3 + var4);
            this.inv.setItem(var4, OnyxShop.getInstance().getSection(var5.section).updateItem(this.player, var5));
         }

         OnyxShop.getInstance().navBar.addSearchNavBar(this.inv, this.player, this.currentQuery.cycle(), var1, this.pages);
         this.open(this.player, UserManager.getUser(this.player));
      }
   }

   private void open(Player var1, User var2) {
      var2.setOpenNewGUI(true);
      var1.openInventory(this.inv);
   }

   public void cycleQueryMethod() {
      this.currentQuery = this.currentQuery.cycle();
   }

   public void newSearch() {
      new AnvilGUI.Builder()
         .onComplete((var1, var2) -> {
            this.search = var2;
            this.getResults(this::init, this.currentQuery, this.search);
            return AnvilGUI.Response.close();
         })
         .onClose(var1 -> OnyxShop.getInstance().runTaskLater(this.player::updateInventory, 1L))
         .itemLeft(new ItemStack(Material.PAPER))
         .text(this.search)
         .title(Lang.SHOPSEARCH_TITLE.get(this.player).getLegacy())
         .plugin(OnyxShop.getInstance())
         .open(this.player);
   }

   public ShopItem getShopItemForSlot(int var1) {
      int var2 = (size == -1 ? 54 : size) - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0);
      int var3 = this.page * var2 - var2;
      return this.results.get(var3 + var1);
   }

   private void getResults(Consumer<LinkedList<ShopItem>> var1, ShopSearch.QueryMethod var2, String var3) {
      if (OnyxShop.getInstance().shopSearch) {
         OnyxShop.getInstance().runTaskAsync(() -> {
            LinkedList var4 = null;
            QueryBuilder var5 = OnyxShop.getInstance().getQueryBuilder();
            switch (var2) {
               case BY_NAME:
                  var4 = var5.queryByName(var3);
                  break;
               case BY_TYPE:
                  var4 = var5.queryByType(var3);
            }

            int var6 = size == -1 ? CalculateAmount.getInvSlots(UserManager.getUser(this.player), var4.size()) : size;
            this.pages = (int)Math.ceil(var4.size() / ((double)var6 - (OnyxShop.getInstance().navBar.isEnableSearchNav() ? 9 : 0)));
            var1.accept(var4);
         });
      }
   }

   @Override
   public Inventory getInventory() {
      return this.inv;
   }

   static {
      int var0 = Math.min(ConfigManager.getConfig().getInt("shop-search-screen.gui-rows", -1), 6);
      size = var0 < 1 ? -1 : var0 * 9;
   }

   public static enum QueryMethod {
      BY_NAME,
      BY_TYPE;

      private QueryMethod() {
      }

      public String getName() {
         String var1 = "SHOPSEARCH_METHOD_" + this.name();
         return Lang.valueOf(var1).get().getLegacy();
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
