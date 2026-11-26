package com.onyxshop.objects.navbar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.CreateItem;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.MainMenu;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPage;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.bedrock.BedrockInventory;
import com.onyxshop.objects.bedrock.inventorys.ShopSection;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.BackButton;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.LocalizedItemStack;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.exceptions.ItemLoadException;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class NavBar extends CreateItem {
   private final OnyxShop plugin;
   private final Map<String, NavBar.InventoryNavBar> shopNavBars = new HashMap<>();
   private NavBar.InventoryNavBar mainMenuNavBar;
   private NavBar.InventoryNavBar shopsNavBar;
   private NavBar.InventoryNavBar transactionNavBar;
   private NavBar.InventoryNavBar shopStandsNavBar;
   private NavBar.InventoryNavBar sellGUINavBar;
   private NavBar.InventoryNavBar searchNavBar;
   private LocalizedItemStack customPlayerProfile;

   public NavBar(OnyxShop var1) {
      super(var1);
      this.plugin = var1;
   }

   public boolean isEnabled(String var1) {
      return this.shopNavBars.containsKey(var1) ? this.shopNavBars.get(var1).enabled : this.shopsNavBar.enabled;
   }

   public boolean isEnableMainNav() {
      return this.mainMenuNavBar.enabled;
   }

   public boolean isEnableTransactionNav() {
      return this.transactionNavBar.enabled;
   }

   public boolean isEnableShopStandsNav() {
      return this.shopStandsNavBar.enabled;
   }

   public boolean isEnableSellGUINav() {
      return this.sellGUINavBar.enabled;
   }

   public boolean isEnableSearchNav() {
      return this.searchNavBar.enabled;
   }

   public ItemStack getCustomPlayerProfile(Player var1) {
      return this.customPlayerProfile != null ? this.customPlayerProfile.getItem(var1) : null;
   }

   public void execute(Player var1, ShopPage var2, int var3, int var4, boolean var5) {
      NavItem var6 = this.getItem(var2.getSection(), var3 - (var2.getSize() - 9));
      if (var6 != null) {
         switch (var6.getAction(var4, var2.getAllPages())) {
            case BACK:
               if (!var5 || var2.isSubSection() && !BackButton.SUB_SECTIONS) {
                  if (var2.isSubSection()) {
                     this.plugin.getSection(var2.getRootSection()).openShopSection(var1, var5);
                  } else {
                     MainMenu.open(var1);
                  }

                  this.plugin.getSoundEffects().useBackButton(var1);
               }
               break;
            case CLOSE:
               this.close(var1);
               this.plugin.getSoundEffects().closeMainMenu(var1);
               break;
            case COMMAND:
               if (var6.isCloseMenu()) {
                  this.close(var1);
               }

               CommandHelper.executeClickCommands(var6.getCommands(var2.getAllPages()), var1);
            case TOGGLE_MODE:
            default:
               break;
            case PAGE_BACK:
               if (var4 > 1) {
                  this.plugin.getSection(var2.getSection()).openShopSection(var1, var4 - 1, var5, var2.getRootSection());
                  this.plugin.getSoundEffects().changePage(var1);
               }
               break;
            case PAGE_NEXT:
               if (var4 != this.plugin.getSection(var2.getSection()).getPages()) {
                  this.plugin.getSection(var2.getSection()).openShopSection(var1, var4 + 1, var5, var2.getRootSection());
                  this.plugin.getSoundEffects().changePage(var1);
               }
               break;
            case SEARCH:
               this.search(var1);
         }
      }
   }

   public void execute(Player var1, ShopPage var2, ActionItem.Action var3, int var4, boolean var5) {
      switch (var3) {
         case BACK:
            if (!var5 || var2.isSubSection() && !BackButton.SUB_SECTIONS) {
               if (var2.isSubSection()) {
                  this.plugin.getSection(var2.getRootSection()).openShopSection(var1, var5);
               } else {
                  MainMenu.open(var1);
               }

               this.plugin.getSoundEffects().useBackButton(var1);
            }
            break;
         case PAGE_BACK:
            if (var4 > 1) {
               this.plugin.getSection(var2.getSection()).openShopSection(var1, var4 - 1, var5, var2.getRootSection());
               this.plugin.getSoundEffects().changePage(var1);
            }
            break;
         case PAGE_NEXT:
            if (var4 != this.plugin.getSection(var2.getSection()).getPages()) {
               this.plugin.getSection(var2.getSection()).openShopSection(var1, var4 + 1, var5, var2.getRootSection());
               this.plugin.getSoundEffects().changePage(var1);
            }
            break;
         case CLOSE:
            this.close(var1);
            this.plugin.getSoundEffects().closeMainMenu(var1);
            break;
         case SEARCH:
            this.search(var1);
      }
   }

   private NavItem getItem(String var1, int var2) {
      return this.shopNavBars.containsKey(var1) ? this.shopNavBars.get(var1).get(var2) : this.shopsNavBar.get(var2);
   }

   public ItemStack[] getShopNavBar(String var1, Player var2, int var3, int var4) {
      return this.shopNavBars.containsKey(var1) ? this.shopNavBars.get(var1).get(var2, var1, var3, var4) : this.shopsNavBar.get(var2, var1, var3, var4);
   }

   public String[] getShopNavTextures(String var1, int var2, int var3) {
      return this.shopNavBars.containsKey(var1) ? this.shopNavBars.get(var1).getBedrockTextures(var2, var3) : this.shopsNavBar.getBedrockTextures(var2, var3);
   }

   public boolean execute(ShopSection var1, Player var2, int var3, int var4) {
      NavItem var5 = this.getItem(var1.getSection(), var3);
      if (var5 == null) {
         return true;
      } else {
         switch (var5.getAction(var4, var1.getAllPages())) {
            case BACK:
               if (!var1.isDisabledBackButton() || var1.isSubSection() && !BackButton.SUB_SECTIONS) {
                  if (var1.isSubSection()) {
                     this.plugin.getSection(var1.getRootSection()).openShopSection(var2, var1.isDisabledBackButton());
                  } else {
                     MainMenu.open(var2);
                  }

                  this.plugin.getSoundEffects().useBackButton(var2);
                  return true;
               } else {
                  return false;
               }
            case CLOSE:
               this.plugin.getSoundEffects().closeMainMenu(var2);
               return true;
            case COMMAND:
               CommandHelper.executeClickCommands(var5.getCommands(var1.getAllPages()), var2);
               return var5.isCloseMenu();
            case TOGGLE_MODE:
            default:
               return false;
            case PAGE_BACK:
               if (var4 > 1) {
                  this.plugin.getSection(var1.getSection()).openShopSection(var2, var4 - 1, var1.isDisabledBackButton(), var1.getRootSection());
                  this.plugin.getSoundEffects().changePage(var2);
                  return true;
               }

               return false;
            case PAGE_NEXT:
               if (var4 != this.plugin.getSection(var1.getSection()).getPages()) {
                  this.plugin.getSection(var1.getSection()).openShopSection(var2, var4 + 1, var1.isDisabledBackButton(), var1.getRootSection());
                  this.plugin.getSoundEffects().changePage(var2);
                  return true;
               }

               return false;
            case SEARCH:
               this.search(var2);
               return true;
         }
      }
   }

   public boolean execute(ShopSection var1, Player var2, ActionItem.Action var3, int var4) {
      switch (var3) {
         case BACK:
            if (!var1.isDisabledBackButton() || var1.isSubSection() && !BackButton.SUB_SECTIONS) {
               if (var1.isSubSection()) {
                  this.plugin.getSection(var1.getRootSection()).openShopSection(var2, var1.isDisabledBackButton());
               } else {
                  MainMenu.open(var2);
               }

               this.plugin.getSoundEffects().useBackButton(var2);
               return true;
            } else {
               return false;
            }
         case PAGE_BACK:
            if (var4 > 1) {
               this.plugin.getSection(var1.getSection()).openShopSection(var2, var4 - 1, var1.isDisabledBackButton(), var1.getRootSection());
               this.plugin.getSoundEffects().changePage(var2);
               return true;
            }

            return false;
         case PAGE_NEXT:
            if (var4 != this.plugin.getSection(var1.getSection()).getPages()) {
               this.plugin.getSection(var1.getSection()).openShopSection(var2, var4 + 1, var1.isDisabledBackButton(), var1.getRootSection());
               this.plugin.getSoundEffects().changePage(var2);
               return true;
            }

            return false;
         case CLOSE:
            this.plugin.getSoundEffects().closeMainMenu(var2);
            return true;
         case SEARCH:
            this.search(var2);
            return true;
         default:
            return false;
      }
   }

   public void execute(InventoryHolder var1, Player var2, int var3) {
      NavItem var4 = var1 instanceof MainMenu ? this.mainMenuNavBar.get(var3) : this.sellGUINavBar.get(var3);
      if (var4 != null) {
         switch (var4.getAction()) {
            case BACK:
            case CLOSE:
               this.close(var2);
               this.plugin.getSoundEffects().closeMainMenu(var2);
               break;
            case COMMAND:
               if (var4.isCloseMenu()) {
                  this.close(var2);
               }

               CommandHelper.executeClickCommands(var4.getCommands(), var2);
            case TOGGLE_MODE:
            case PAGE_BACK:
            case PAGE_NEXT:
            default:
               break;
            case SEARCH:
               this.search(var2);
         }
      }
   }

   public boolean execute(BedrockInventory var1, Player var2, int var3) {
      NavItem var4 = var1 instanceof com.onyxshop.objects.bedrock.inventorys.MainMenu
         ? this.mainMenuNavBar.get(var3)
         : this.sellGUINavBar.get(var3);
      if (var4 == null) {
         return true;
      } else {
         switch (var4.getAction()) {
            case BACK:
            case CLOSE:
               this.plugin.getSoundEffects().closeMainMenu(var2);
               return true;
            case COMMAND:
               CommandHelper.executeClickCommands(var4.getCommands(), var2);
               return var4.isCloseMenu();
            case TOGGLE_MODE:
            case PAGE_BACK:
            case PAGE_NEXT:
            default:
               return false;
            case SEARCH:
               this.search(var2);
               return true;
         }
      }
   }

   public void execute(
      TransactionMenu var1, Player var2, ShopItem var3, int var4, String var5, boolean var6, Transaction.Mode var7, Transaction.Type var8, int var9
   ) {
      NavItem var10 = this.transactionNavBar.get(var4);
      if (var10 != null) {
         switch (var10.getAction()) {
            case BACK:
               if (var8 != Transaction.Type.BUY_STACKS_SCREEN) {
                  com.onyxshop.objects.shops.ShopSection var11 = this.plugin.getSection(var3.section);
                  var11.openShopSection(var2, var11.getPageForShopItem(var3.itemLoc), var6, var5);
                  this.plugin.getSoundEffects().useBackButton(var2);
               } else {
                  new TransactionMenu(var2, var3, var5, var6, Transaction.Mode.getFromType(var8), Transaction.Type.BUY_SCREEN, var9).open(false);
               }
               break;
            case CLOSE:
               this.close(var2);
               this.plugin.getSoundEffects().closeMainMenu(var2);
               break;
            case COMMAND:
               if (var10.isCloseMenu()) {
                  this.close(var2);
               }

               CommandHelper.executeClickCommands(var10.getCommands(), var2);
               break;
            case TOGGLE_MODE:
               if (!var10.isOnlyGeyser() || UserManager.getUser(var2).isBedrock()) {
                  new TransactionMenu(
                        var2,
                        var3,
                        var5,
                        var6,
                        var7 == Transaction.Mode.BUY ? Transaction.Mode.SELL : Transaction.Mode.BUY,
                        var7 == Transaction.Mode.BUY ? Transaction.Type.SELL_SCREEN : Transaction.Type.BUY_SCREEN,
                        var9
                     )
                     .open(false);
               }
            case PAGE_BACK:
            case PAGE_NEXT:
            case CHANGE_SEARCH_MODE:
            default:
               break;
            case SEARCH:
               this.search(var2);
               break;
            case CYCLE_CURRENCY:
               if (var7 == Transaction.Mode.BUY && var3.isABuyPricing() || var7 == Transaction.Mode.SELL && var3.isASellPricing()) {
                  var1.cycleEcoType();
               }
         }
      }
   }

   public void execute(TransactionMenu var1, Player var2, ShopItem var3, int var4, Transaction.Mode var5, int var6) {
      NavItem var7 = this.shopStandsNavBar.get(var4);
      if (var7 != null) {
         switch (var7.getAction()) {
            case CLOSE:
               this.close(var2);
               this.plugin.getSoundEffects().closeMainMenu(var2);
               break;
            case COMMAND:
               if (var7.isCloseMenu()) {
                  this.close(var2);
               }

               CommandHelper.executeClickCommands(var7.getCommands(), var2);
               break;
            case TOGGLE_MODE:
               if (!var7.isOnlyGeyser() || UserManager.getUser(var2).isBedrock()) {
                  new TransactionMenu(
                        var2,
                        var3,
                        null,
                        false,
                        var5 == Transaction.Mode.BUY ? Transaction.Mode.SELL : Transaction.Mode.BUY,
                        var5 == Transaction.Mode.BUY ? Transaction.Type.SHOPSTAND_SELL_SCREEN : Transaction.Type.SHOPSTAND_BUY_SCREEN,
                        var6
                     )
                     .open(false);
               }
            case PAGE_BACK:
            case PAGE_NEXT:
            case CHANGE_SEARCH_MODE:
            default:
               break;
            case SEARCH:
               this.search(var2);
               break;
            case CYCLE_CURRENCY:
               if (var5 == Transaction.Mode.BUY && var3.isABuyPricing() || var5 == Transaction.Mode.SELL && var3.isASellPricing()) {
                  var1.cycleEcoType();
               }
         }
      }
   }

   public void execute(ShopSearch var1, Player var2, int var3, int var4) {
      NavItem var5 = this.searchNavBar.get(var3);
      if (var5 != null) {
         switch (var5.getAction(var4, var1.getPages())) {
            case BACK:
               var1.newSearch();
               this.plugin.getSoundEffects().useBackButton(var2);
               break;
            case CLOSE:
               this.close(var2);
               this.plugin.getSoundEffects().closeMainMenu(var2);
               break;
            case COMMAND:
               if (var5.isCloseMenu()) {
                  this.close(var2);
               }

               CommandHelper.executeClickCommands(var5.getCommands(), var2);
            case TOGGLE_MODE:
            default:
               break;
            case PAGE_BACK:
               if (var4 > 1) {
                  var1.open(var4 - 1);
                  this.plugin.getSoundEffects().changePage(var2);
               }
               break;
            case PAGE_NEXT:
               if (var4 < var1.getPages()) {
                  var1.open(var4 + 1);
                  this.plugin.getSoundEffects().changePage(var2);
               }
               break;
            case SEARCH:
               this.search(var2);
               break;
            case CHANGE_SEARCH_MODE:
               var1.cycleQueryMethod();
               var1.newSearch();
         }
      }
   }

   private void close(Player var1) {
      UserManager.getUser(var1).setOpenNewGUI(true);
      var1.closeInventory();
   }

   private void search(Player var1) {
      new AnvilGUI.Builder()
         .onComplete((var0, var1x) -> {
            new ShopSearch(var0, var1x);
            return AnvilGUI.Response.close();
         })
         .onClose(var0 -> OnyxShop.getInstance().runTaskLater(var0::updateInventory, 1L))
         .itemLeft(new ItemStack(Material.NAME_TAG))
         .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(var1).getLegacy())
         .title(Lang.SHOPSEARCH_TITLE.get(var1).getLegacy())
         .plugin(OnyxShop.getInstance())
         .open(var1);
   }

   public void loadNavBars() {
      this.mainMenuNavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("main-menu-nav-bar"));
      this.transactionNavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("transaction-screens-nav-bar"));
      this.sellGUINavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("sellgui-nav-bar"));
      this.searchNavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("shop-search-nav-bar"));
      this.loadShopNavBars();
      if (this.plugin.shopStands) {
         this.shopStandsNavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("shopstands-transaction-screens-nav-bar"));
      }
   }

   private void loadShopNavBars() {
      if (!this.shopNavBars.isEmpty()) {
         this.shopNavBars.clear();
      }

      this.shopsNavBar = new NavBar.InventoryNavBar(ConfigManager.getConfig().getConfigurationSection("shops-nav-bar"));
      this.plugin.getShopSections().forEach(this::loadShopNavBar);
   }

   private void loadShopNavBar(String var1) {
      if (!ConfigManager.getSection(var1).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("INHERIT")) {
         this.shopNavBars.put(var1, new NavBar.InventoryNavBar(var1));
      }
   }

   private NavItem getDefaultItem(String var1, ItemStack var2) throws ItemLoadException {
      try {
         return this.getNavBarItem(ConfigManager.getConfig().getConfigurationSection(var1), var2);
      } catch (ItemLoadException var4) {
         SendMessage.logDebugMessage(var4.getMessage());
         SendMessage.errorItemConfig(var1);
         return new NavBarItem(ConfigManager.getConfig().getDef().getConfigurationSection(var1), var2);
      }
   }

   private NavItem getShopItem(String var1, String var2, ItemStack var3) {
      try {
         return this.getNavBarItem(ConfigManager.getSection(var1).getConfigurationSection(var2), var3);
      } catch (ItemLoadException var5) {
         SendMessage.logDebugMessage(var5.getMessage());
         SendMessage.errorSections(var1, var2);
         return null;
      }
   }

   private NavItem getNavBarItem(ConfigurationSection var1, ItemStack var2) throws ItemLoadException {
      if (var1 != null && !var1.getKeys(false).isEmpty()) {
         String var3 = var1.getString("skullowner");
         return (NavItem)(var3 != null && var3.equals("%player_name%") ? new PlayerProfile(var1, var2) : new NavBarItem(var1, var2));
      } else {
         throw new ItemLoadException("Could not create the navigation-bar item because it was not found.");
      }
   }

   public Inventory addShopsNavBar(Inventory var1, Player var2, String var3, int var4, int var5) {
      return this.shopNavBars.containsKey(var3)
         ? this.shopNavBars.get(var3).load(var1, var2, var3, var4, var5)
         : this.shopsNavBar.load(var1, var2, var3, var4, var5);
   }

   public Inventory updateShopsNavBar(Inventory var1, Player var2, String var3, int var4, int var5) {
      return this.shopNavBars.containsKey(var3)
         ? this.shopNavBars.get(var3).update(var1, var2, var3, var4, var5)
         : this.shopsNavBar.update(var1, var2, var3, var4, var5);
   }

   public Inventory addMainMenuNavBar(Inventory var1, Player var2) {
      return this.mainMenuNavBar.load(var1, var2);
   }

   public ItemStack[] getMainMenuNavBar(Player var1) {
      return this.mainMenuNavBar.get(var1);
   }

   public String[] getMainMenuNavTextures() {
      return this.mainMenuNavBar.getBedrockTextures();
   }

   public Inventory addSellGUINavBar(Inventory var1, Player var2) {
      return this.sellGUINavBar.load(var1, var2);
   }

   public Inventory addTransactionNavBar(Inventory var1, Transaction.Mode var2, Player var3, ShopItem var4, EcoType var5) {
      return this.transactionNavBar.load(var1, var2, var3, var4, var5);
   }

   public Inventory addShopStandsNavBar(Inventory var1, Transaction.Mode var2, Player var3, ShopItem var4, EcoType var5) {
      return this.shopStandsNavBar.load(var1, var2, var3, var4, var5);
   }

   public Inventory addSearchNavBar(Inventory var1, Player var2, ShopSearch.QueryMethod var3, int var4, int var5) {
      return this.searchNavBar.load(var1, var2, var3, var4, var5);
   }

   public void createCustomPlayerProfile() {
      if (!ConfigManager.getConfig().getBoolean("custom-bedrock-player-profile.enabled", true)) {
         this.customPlayerProfile = null;
      } else {
         ConfigurationSection var1 = ConfigManager.getConfig().getConfigurationSection("custom-bedrock-player-profile.item");
         if (var1 != null && !var1.getKeys(false).isEmpty()) {
            this.customPlayerProfile = this.createFillItem(var1, null);
         } else {
            this.customPlayerProfile = null;
         }
      }
   }

   private class InventoryNavBar {
      private boolean enabled;
      private ItemStack fillItem;
      private final Set<Integer> fillItems = new HashSet<>();
      private final Map<Integer, NavItem> items = new HashMap<>();

      public NavItem get(int var1) {
         return this.items.get(var1);
      }

      public InventoryNavBar(String nullx) {
         this.enabled = !ConfigManager.getSection(nullx).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("DISABLED");
         if (this.enabled) {
            LocalizedItemStack var3 = ConfigManager.getSection(nullx).contains("nav-bar.fill-item")
               ? NavBar.this.plugin.createItem.createFillItem(ConfigManager.getSection(nullx).getConfigurationSection("nav-bar.fill-item"), nullx)
               : null;
            this.fillItem = var3 == null ? null : var3.getItem();
            ConfigurationSection var4 = ConfigManager.getSection(nullx).getConfigurationSection("nav-bar.items");
            if (var4 != null && !var4.getKeys(false).isEmpty()) {
               int var5 = 1;

               for (String var7 : var4.getKeys(false)) {
                  NavItem var8 = NavBar.this.getShopItem(nullx, "nav-bar.items." + var7, this.fillItem);
                  if (var8 == null) {
                     var8 = NavBar.this.shopsNavBar.get(var5);
                  }

                  int var9 = 0;

                  try {
                     if (ConfigManager.getSection(nullx).contains("nav-bar.items." + var7 + ".slot")) {
                        var9 = ConfigManager.getSection(nullx).getInt("nav-bar.items." + var7 + ".slot");
                     } else {
                        try {
                           var9 = Integer.parseInt(var7);

                           while (this.items.containsKey(var9 - 1)) {
                              var9++;
                           }
                        } catch (NumberFormatException var11) {
                           var9 = var5;

                           while (this.items.containsKey(var9 - 1)) {
                              var9++;
                           }
                        }
                     }
                  } catch (NumberFormatException var12) {
                     SendMessage.warnMessage(
                        "Cannot create navigation bar item for shop '"
                           + nullx
                           + "' at 'nav-bar.items."
                           + var7
                           + "' because the slot is not a valid integer. Got '"
                           + ConfigManager.getSection(nullx).getInt("nav-bar.items." + var7 + ".slot")
                           + "'"
                     );
                  }

                  if (var9 <= 9 && var9 >= 1) {
                     this.items.put(var9 - 1, var8);
                     var5++;
                  } else {
                     SendMessage.warnMessage(
                        "Cannot create navigation bar item for shop '"
                           + nullx
                           + "' at 'nav-bar.items."
                           + var7
                           + "' because the slot is out of bounds for "
                           + var9
                           + ". Valid values should be in range of 1-9"
                     );
                  }
               }

               if (this.items.isEmpty()) {
                  this.enabled = false;
               }
            } else {
               this.enabled = false;
            }
         }
      }

      public InventoryNavBar(ConfigurationSection nullx) {
         this.enabled = nullx.getBoolean("enabled", true);
         if (this.enabled) {
            LocalizedItemStack var3 = nullx.contains("fill-item")
               ? NavBar.this.plugin.createItem.createFillItem(nullx.getConfigurationSection("fill-item"), null)
               : null;
            this.fillItem = var3 == null ? null : var3.getItem();
            nullx = nullx.getConfigurationSection("items");
            if (nullx != null && !nullx.getKeys(false).isEmpty()) {
               int var4 = 1;

               for (String var6 : nullx.getKeys(false)) {
                  try {
                     NavItem var7 = NavBar.this.getDefaultItem(nullx.getCurrentPath() + "." + var6, this.fillItem);
                     int var8 = 0;

                     try {
                        if (nullx.contains(var6 + ".slot")) {
                           var8 = nullx.getInt(var6 + ".slot");
                        } else {
                           try {
                              var8 = Integer.parseInt(var6);

                              while (this.items.containsKey(var8 - 1)) {
                                 var8++;
                              }
                           } catch (NumberFormatException var10) {
                              var8 = var4;

                              while (this.items.containsKey(var8 - 1)) {
                                 var8++;
                              }
                           }
                        }
                     } catch (NumberFormatException var11) {
                        SendMessage.warnMessage(
                           "Cannot create navigation bar item at '"
                              + nullx.getCurrentPath()
                              + "."
                              + var6
                              + "' because the slot is not a valid integer. Got '"
                              + nullx.getString(var6 + ".slot")
                              + "'"
                        );
                     }

                     if (var8 <= 9 && var8 >= 1) {
                        this.items.put(var8 - 1, var7);
                        var4++;
                     } else {
                        SendMessage.warnMessage(
                           "Cannot create navigation bar item at '"
                              + nullx.getCurrentPath()
                              + "."
                              + var6
                              + "' because the slot is out of bounds for "
                              + var8
                              + ". Valid values should be in range of 1-9"
                        );
                     }
                  } catch (ItemLoadException var12) {
                  }
               }

               if (this.items.isEmpty()) {
                  this.enabled = false;
               }
            } else {
               this.enabled = false;
            }
         }
      }

      public Inventory load(Inventory var1, Player var2, String var3, int var4, int var5) {
         if (!this.enabled) {
            return var1;
         } else {
            int var6 = var1.getSize();

            for (int var7 = 0; var7 < 9; var7++) {
               if (this.items.containsKey(var7)) {
                  var1.setItem(var6 - 9 + var7, this.items.get(var7).getItem(var2, NavBar.this.plugin.getEcoHandler().getEcon(var3).getType(), var4, var5));
               } else if (this.fillItem != null) {
                  var1.setItem(var6 - 9 + var7, this.fillItem);
                  this.fillItems.add(var7);
               } else {
                  var1.setItem(var6 - 9 + var7, new ItemStack(Material.AIR));
               }
            }

            return var1;
         }
      }

      public Inventory update(Inventory var1, Player var2, String var3, int var4, int var5) {
         if (!this.enabled) {
            return var1;
         } else {
            int var6 = var1.getSize();

            for (int var7 = 0; var7 < 9; var7++) {
               if (this.items.containsKey(var7)) {
                  NavItem var8 = this.items.get(var7);
                  if (!var8.isStatic(var4, var5)) {
                     var1.setItem(var6 - 9 + var7, var8.getItem(var2, NavBar.this.plugin.getEcoHandler().getEcon(var3).getType(), var4, var5));
                  }
               } else if (this.fillItem != null) {
                  if (!this.fillItems.contains(var7)) {
                     var1.setItem(var6 - 9 + var7, this.fillItem);
                  }
               } else {
                  var1.setItem(var6 - 9 + var7, new ItemStack(Material.AIR));
               }
            }

            return var1;
         }
      }

      public ItemStack[] get(Player var1, String var2, int var3, int var4) {
         if (!this.enabled) {
            return null;
         } else {
            ItemStack[] var5 = new ItemStack[9];

            for (Entry<Integer, NavItem> var7 : this.items.entrySet()) {
               var5[var7.getKey()] = ((NavItem)var7.getValue()).getItem(var1, NavBar.this.plugin.getEcoHandler().getEcon(var2).getType(), var3, var4);
            }

            if (this.fillItem != null) {
               for (int var8 = 0; var8 < 9; var8++) {
                  if (!this.items.containsKey(var8)) {
                     var5[var8] = this.fillItem;
                  }
               }
            }

            return var5;
         }
      }

      public String[] getBedrockTextures(int var1, int var2) {
         if (!this.enabled) {
            return null;
         } else {
            String[] var3 = new String[9];

            for (Entry<Integer, NavItem> var5 : this.items.entrySet()) {
               var3[var5.getKey()] = ((NavItem)var5.getValue()).getBedrockTexture(var1, var2);
            }

            return var3;
         }
      }

      public ItemStack[] get(Player var1) {
         if (!this.enabled) {
            return null;
         } else {
            ItemStack[] var2 = new ItemStack[9];

            for (Entry<Integer, NavItem> var4 : this.items.entrySet()) {
               var2[var4.getKey()] = ((NavItem)var4.getValue()).getItem(var1);
            }

            if (this.fillItem != null) {
               for (int var5 = 0; var5 < 9; var5++) {
                  if (!this.items.containsKey(var5)) {
                     var2[var5] = this.fillItem;
                  }
               }
            }

            return var2;
         }
      }

      public String[] getBedrockTextures() {
         if (!this.enabled) {
            return null;
         } else {
            String[] var1 = new String[9];

            for (Entry<Integer, NavItem> var3 : this.items.entrySet()) {
               var1[var3.getKey()] = ((NavItem)var3.getValue()).getBedrockTexture();
            }

            return var1;
         }
      }

      public Inventory load(Inventory var1, Player var2) {
         if (!this.enabled) {
            return var1;
         } else {
            for (Entry var4 : this.items.entrySet()) {
               var1.setItem(var1.getSize() - 9 + (Integer)var4.getKey(), ((NavItem)var4.getValue()).getItem(var2));
            }

            if (this.fillItem != null) {
               for (int var5 = 0; var5 < 9; var5++) {
                  if (!this.items.containsKey(var5)) {
                     var1.setItem(var1.getSize() - 9 + var5, this.fillItem);
                  }
               }
            }

            return var1;
         }
      }

      public Inventory load(Inventory var1, Transaction.Mode var2, Player var3, ShopItem var4, EcoType var5) {
         if (!this.enabled) {
            return var1;
         } else {
            for (Entry var7 : this.items.entrySet()) {
               NavItem var8 = (NavItem)var7.getValue();
               if (!var8.isDisabled()) {
                  var1.setItem(var1.getSize() - 9 + (Integer)var7.getKey(), var8.getItem(var3, var2, var4, var5));
               }
            }

            if (this.fillItem != null) {
               for (int var9 = 0; var9 < 9; var9++) {
                  if (!this.items.containsKey(var9)) {
                     var1.setItem(var1.getSize() - 9 + var9, this.fillItem);
                  }
               }
            }

            return var1;
         }
      }

      public Inventory load(Inventory var1, Player var2, ShopSearch.QueryMethod var3, int var4, int var5) {
         if (!this.enabled) {
            return var1;
         } else {
            for (Entry var7 : this.items.entrySet()) {
               var1.setItem(var1.getSize() - 9 + (Integer)var7.getKey(), ((NavItem)var7.getValue()).getItem(var2, null, var3, var4, var5));
            }

            if (this.fillItem != null) {
               for (int var8 = 0; var8 < 9; var8++) {
                  if (!this.items.containsKey(var8)) {
                     var1.setItem(var1.getSize() - 9 + var8, this.fillItem);
                  }
               }
            }

            return var1;
         }
      }
   }

   public static enum ItemAction {
      BACK,
      CLOSE,
      COMMAND,
      TOGGLE_MODE,
      PAGE_BACK,
      PAGE_NEXT,
      SEARCH,
      CHANGE_SEARCH_MODE,
      CYCLE_CURRENCY,
      NONE;

      private ItemAction() {
      }

      public static NavBar.ItemAction getFromString(String var0) {
         try {
            return valueOf(var0.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
         } catch (IllegalArgumentException var2) {
            return NONE;
         }
      }
   }
}
