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

   public NavBar(OnyxShop plugin) {
      super(plugin);
      this.plugin = plugin;
   }

   public boolean isEnabled(String text) {
      return this.shopNavBars.containsKey(text) ? this.shopNavBars.get(text).enabled : this.shopsNavBar.enabled;
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

   public ItemStack getCustomPlayerProfile(Player player) {
      return this.customPlayerProfile != null ? this.customPlayerProfile.getItem(player) : null;
   }

   public void execute(Player player, ShopPage shopPage, int page, int value, boolean flag) {
      NavItem navItem = this.getItem(shopPage.getSection(), page - (shopPage.getSize() - 9));
      if (navItem != null) {
         switch (navItem.getAction(value, shopPage.getAllPages())) {
            case BACK:
               if (!flag || shopPage.isSubSection() && !BackButton.SUB_SECTIONS) {
                  if (shopPage.isSubSection()) {
                     this.plugin.getSection(shopPage.getRootSection()).openShopSection(player, flag);
                  } else {
                     MainMenu.open(player);
                  }

                  this.plugin.getSoundEffects().useBackButton(player);
               }
               break;
            case CLOSE:
               this.close(player);
               this.plugin.getSoundEffects().closeMainMenu(player);
               break;
            case COMMAND:
               if (navItem.isCloseMenu()) {
                  this.close(player);
               }

               CommandHelper.executeClickCommands(navItem.getCommands(shopPage.getAllPages()), player);
            case TOGGLE_MODE:
            default:
               break;
            case PAGE_BACK:
               if (value > 1) {
                  this.plugin.getSection(shopPage.getSection()).openShopSection(player, value - 1, flag, shopPage.getRootSection());
                  this.plugin.getSoundEffects().changePage(player);
               }
               break;
            case PAGE_NEXT:
               if (value != this.plugin.getSection(shopPage.getSection()).getPages()) {
                  this.plugin.getSection(shopPage.getSection()).openShopSection(player, value + 1, flag, shopPage.getRootSection());
                  this.plugin.getSoundEffects().changePage(player);
               }
               break;
            case SEARCH:
               this.search(player);
         }
      }
   }

   public void execute(Player player, ShopPage shopPage, ActionItem.Action action, int page, boolean flag) {
      switch (action) {
         case BACK:
            if (!flag || shopPage.isSubSection() && !BackButton.SUB_SECTIONS) {
               if (shopPage.isSubSection()) {
                  this.plugin.getSection(shopPage.getRootSection()).openShopSection(player, flag);
               } else {
                  MainMenu.open(player);
               }

               this.plugin.getSoundEffects().useBackButton(player);
            }
            break;
         case PAGE_BACK:
            if (page > 1) {
               this.plugin.getSection(shopPage.getSection()).openShopSection(player, page - 1, flag, shopPage.getRootSection());
               this.plugin.getSoundEffects().changePage(player);
            }
            break;
         case PAGE_NEXT:
            if (page != this.plugin.getSection(shopPage.getSection()).getPages()) {
               this.plugin.getSection(shopPage.getSection()).openShopSection(player, page + 1, flag, shopPage.getRootSection());
               this.plugin.getSoundEffects().changePage(player);
            }
            break;
         case CLOSE:
            this.close(player);
            this.plugin.getSoundEffects().closeMainMenu(player);
            break;
         case SEARCH:
            this.search(player);
      }
   }

   private NavItem getItem(String itemId, int value) {
      return this.shopNavBars.containsKey(itemId) ? this.shopNavBars.get(itemId).get(value) : this.shopsNavBar.get(value);
   }

   public ItemStack[] getShopNavBar(String shopId, Player player, int value, int value2) {
      return this.shopNavBars.containsKey(shopId) ? this.shopNavBars.get(shopId).get(player, shopId, value, value2) : this.shopsNavBar.get(player, shopId, value, value2);
   }

   public String[] getShopNavTextures(String shopId, int value, int value2) {
      return this.shopNavBars.containsKey(shopId) ? this.shopNavBars.get(shopId).getBedrockTextures(value, value2) : this.shopsNavBar.getBedrockTextures(value, value2);
   }

   public boolean execute(ShopSection section, Player player, int value, int value2) {
      NavItem navItem = this.getItem(section.getSection(), value);
      if (navItem == null) {
         return true;
      } else {
         switch (navItem.getAction(value2, section.getAllPages())) {
            case BACK:
               if (!section.isDisabledBackButton() || section.isSubSection() && !BackButton.SUB_SECTIONS) {
                  if (section.isSubSection()) {
                     this.plugin.getSection(section.getRootSection()).openShopSection(player, section.isDisabledBackButton());
                  } else {
                     MainMenu.open(player);
                  }

                  this.plugin.getSoundEffects().useBackButton(player);
                  return true;
               } else {
                  return false;
               }
            case CLOSE:
               this.plugin.getSoundEffects().closeMainMenu(player);
               return true;
            case COMMAND:
               CommandHelper.executeClickCommands(navItem.getCommands(section.getAllPages()), player);
               return navItem.isCloseMenu();
            case TOGGLE_MODE:
            default:
               return false;
            case PAGE_BACK:
               if (value2 > 1) {
                  this.plugin.getSection(section.getSection()).openShopSection(player, value2 - 1, section.isDisabledBackButton(), section.getRootSection());
                  this.plugin.getSoundEffects().changePage(player);
                  return true;
               }

               return false;
            case PAGE_NEXT:
               if (value2 != this.plugin.getSection(section.getSection()).getPages()) {
                  this.plugin.getSection(section.getSection()).openShopSection(player, value2 + 1, section.isDisabledBackButton(), section.getRootSection());
                  this.plugin.getSoundEffects().changePage(player);
                  return true;
               }

               return false;
            case SEARCH:
               this.search(player);
               return true;
         }
      }
   }

   public boolean execute(ShopSection section, Player player, ActionItem.Action action, int value) {
      switch (action) {
         case BACK:
            if (!section.isDisabledBackButton() || section.isSubSection() && !BackButton.SUB_SECTIONS) {
               if (section.isSubSection()) {
                  this.plugin.getSection(section.getRootSection()).openShopSection(player, section.isDisabledBackButton());
               } else {
                  MainMenu.open(player);
               }

               this.plugin.getSoundEffects().useBackButton(player);
               return true;
            } else {
               return false;
            }
         case PAGE_BACK:
            if (value > 1) {
               this.plugin.getSection(section.getSection()).openShopSection(player, value - 1, section.isDisabledBackButton(), section.getRootSection());
               this.plugin.getSoundEffects().changePage(player);
               return true;
            }

            return false;
         case PAGE_NEXT:
            if (value != this.plugin.getSection(section.getSection()).getPages()) {
               this.plugin.getSection(section.getSection()).openShopSection(player, value + 1, section.isDisabledBackButton(), section.getRootSection());
               this.plugin.getSoundEffects().changePage(player);
               return true;
            }

            return false;
         case CLOSE:
            this.plugin.getSoundEffects().closeMainMenu(player);
            return true;
         case SEARCH:
            this.search(player);
            return true;
         default:
            return false;
      }
   }

   public void execute(InventoryHolder holder, Player player, int value) {
      NavItem navItem = holder instanceof MainMenu ? this.mainMenuNavBar.get(value) : this.sellGUINavBar.get(value);
      if (navItem != null) {
         switch (navItem.getAction()) {
            case BACK:
            case CLOSE:
               this.close(player);
               this.plugin.getSoundEffects().closeMainMenu(player);
               break;
            case COMMAND:
               if (navItem.isCloseMenu()) {
                  this.close(player);
               }

               CommandHelper.executeClickCommands(navItem.getCommands(), player);
            case TOGGLE_MODE:
            case PAGE_BACK:
            case PAGE_NEXT:
            default:
               break;
            case SEARCH:
               this.search(player);
         }
      }
   }

   public boolean execute(BedrockInventory bedrockInventory, Player player, int value) {
      NavItem navItem = bedrockInventory instanceof com.onyxshop.objects.bedrock.inventorys.MainMenu
         ? this.mainMenuNavBar.get(value)
         : this.sellGUINavBar.get(value);
      if (navItem == null) {
         return true;
      } else {
         switch (navItem.getAction()) {
            case BACK:
            case CLOSE:
               this.plugin.getSoundEffects().closeMainMenu(player);
               return true;
            case COMMAND:
               CommandHelper.executeClickCommands(navItem.getCommands(), player);
               return navItem.isCloseMenu();
            case TOGGLE_MODE:
            case PAGE_BACK:
            case PAGE_NEXT:
            default:
               return false;
            case SEARCH:
               this.search(player);
               return true;
         }
      }
   }

   public void execute(
      TransactionMenu transactionMenu, Player player, ShopItem shopItem, int value, String playerName, boolean flag, Transaction.Mode mode, Transaction.Type type, int value2
   ) {
      NavItem navItem = this.transactionNavBar.get(value);
      if (navItem != null) {
         switch (navItem.getAction()) {
            case BACK:
               if (type != Transaction.Type.BUY_STACKS_SCREEN) {
                  com.onyxshop.objects.shops.ShopSection section = this.plugin.getSection(shopItem.section);
                  section.openShopSection(player, section.getPageForShopItem(shopItem.itemLoc), flag, playerName);
                  this.plugin.getSoundEffects().useBackButton(player);
               } else {
                  new TransactionMenu(player, shopItem, playerName, flag, Transaction.Mode.getFromType(type), Transaction.Type.BUY_SCREEN, value2).open(false);
               }
               break;
            case CLOSE:
               this.close(player);
               this.plugin.getSoundEffects().closeMainMenu(player);
               break;
            case COMMAND:
               if (navItem.isCloseMenu()) {
                  this.close(player);
               }

               CommandHelper.executeClickCommands(navItem.getCommands(), player);
               break;
            case TOGGLE_MODE:
               if (!navItem.isOnlyGeyser() || UserManager.getUser(player).isBedrock()) {
                  new TransactionMenu(
                        player,
                        shopItem,
                        playerName,
                        flag,
                        mode == Transaction.Mode.BUY ? Transaction.Mode.SELL : Transaction.Mode.BUY,
                        mode == Transaction.Mode.BUY ? Transaction.Type.SELL_SCREEN : Transaction.Type.BUY_SCREEN,
                        value2
                     )
                     .open(false);
               }
            case PAGE_BACK:
            case PAGE_NEXT:
            case CHANGE_SEARCH_MODE:
            default:
               break;
            case SEARCH:
               this.search(player);
               break;
            case CYCLE_CURRENCY:
               if (mode == Transaction.Mode.BUY && shopItem.isABuyPricing() || mode == Transaction.Mode.SELL && shopItem.isASellPricing()) {
                  transactionMenu.cycleEcoType();
               }
         }
      }
   }

   public void execute(TransactionMenu transactionMenu, Player player, ShopItem shopItem, int value, Transaction.Mode mode, int value2) {
      NavItem navItem = this.shopStandsNavBar.get(value);
      if (navItem != null) {
         switch (navItem.getAction()) {
            case CLOSE:
               this.close(player);
               this.plugin.getSoundEffects().closeMainMenu(player);
               break;
            case COMMAND:
               if (navItem.isCloseMenu()) {
                  this.close(player);
               }

               CommandHelper.executeClickCommands(navItem.getCommands(), player);
               break;
            case TOGGLE_MODE:
               if (!navItem.isOnlyGeyser() || UserManager.getUser(player).isBedrock()) {
                  new TransactionMenu(
                        player,
                        shopItem,
                        null,
                        false,
                        mode == Transaction.Mode.BUY ? Transaction.Mode.SELL : Transaction.Mode.BUY,
                        mode == Transaction.Mode.BUY ? Transaction.Type.SHOPSTAND_SELL_SCREEN : Transaction.Type.SHOPSTAND_BUY_SCREEN,
                        value2
                     )
                     .open(false);
               }
            case PAGE_BACK:
            case PAGE_NEXT:
            case CHANGE_SEARCH_MODE:
            default:
               break;
            case SEARCH:
               this.search(player);
               break;
            case CYCLE_CURRENCY:
               if (mode == Transaction.Mode.BUY && shopItem.isABuyPricing() || mode == Transaction.Mode.SELL && shopItem.isASellPricing()) {
                  transactionMenu.cycleEcoType();
               }
         }
      }
   }

   public void execute(ShopSearch shopSearch, Player player, int value, int value2) {
      NavItem navItem = this.searchNavBar.get(value);
      if (navItem != null) {
         switch (navItem.getAction(value2, shopSearch.getPages())) {
            case BACK:
               shopSearch.newSearch();
               this.plugin.getSoundEffects().useBackButton(player);
               break;
            case CLOSE:
               this.close(player);
               this.plugin.getSoundEffects().closeMainMenu(player);
               break;
            case COMMAND:
               if (navItem.isCloseMenu()) {
                  this.close(player);
               }

               CommandHelper.executeClickCommands(navItem.getCommands(), player);
            case TOGGLE_MODE:
            default:
               break;
            case PAGE_BACK:
               if (value2 > 1) {
                  shopSearch.open(value2 - 1);
                  this.plugin.getSoundEffects().changePage(player);
               }
               break;
            case PAGE_NEXT:
               if (value2 < shopSearch.getPages()) {
                  shopSearch.open(value2 + 1);
                  this.plugin.getSoundEffects().changePage(player);
               }
               break;
            case SEARCH:
               this.search(player);
               break;
            case CHANGE_SEARCH_MODE:
               shopSearch.cycleQueryMethod();
               shopSearch.newSearch();
         }
      }
   }

   private void close(Player player) {
      UserManager.getUser(player).setOpenNewGUI(true);
      player.closeInventory();
   }

   private void search(Player player) {
      new AnvilGUI.Builder()
         .onComplete((player2, input) -> {
            new ShopSearch(player2, input);
            return AnvilGUI.Response.close();
         })
         .onClose(player2 -> OnyxShop.getInstance().runTaskLaterFor(player2::updateInventory, player2, 1L))
         .itemLeft(new ItemStack(Material.NAME_TAG))
         .text(Lang.SHOPSEARCH_PRE_ENTERED_VALUE.get(player).getLegacy())
         .title(Lang.SHOPSEARCH_TITLE.get(player).getLegacy())
         .plugin(OnyxShop.getInstance())
         .open(player);
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

   private void loadShopNavBar(String shopId) {
      if (!ConfigManager.getSection(shopId).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("INHERIT")) {
         this.shopNavBars.put(shopId, new NavBar.InventoryNavBar(shopId));
      }
   }

   private NavItem getDefaultItem(String itemId, ItemStack itemStack) throws ItemLoadException {
      try {
         return this.getNavBarItem(ConfigManager.getConfig().getConfigurationSection(itemId), itemStack);
      } catch (ItemLoadException exception) {
         SendMessage.logDebugMessage(exception.getMessage());
         SendMessage.errorItemConfig(itemId);
         return new NavBarItem(ConfigManager.getConfig().getDef().getConfigurationSection(itemId), itemStack);
      }
   }

   private NavItem getShopItem(String shopId, String itemId, ItemStack itemStack) {
      try {
         return this.getNavBarItem(ConfigManager.getSection(shopId).getConfigurationSection(itemId), itemStack);
      } catch (ItemLoadException exception) {
         SendMessage.logDebugMessage(exception.getMessage());
         SendMessage.errorSections(shopId, itemId);
         return null;
      }
   }

   private NavItem getNavBarItem(ConfigurationSection section, ItemStack itemStack) throws ItemLoadException {
      if (section != null && !section.getKeys(false).isEmpty()) {
         String text = section.getString("skullowner");
         return (NavItem)(text != null && text.equals("%player_name%") ? new PlayerProfile(section, itemStack) : new NavBarItem(section, itemStack));
      } else {
         throw new ItemLoadException("Could not create the navigation-bar item because it was not found.");
      }
   }

   public Inventory addShopsNavBar(Inventory inventory, Player player, String playerName, int value, int value2) {
      return this.shopNavBars.containsKey(playerName)
         ? this.shopNavBars.get(playerName).load(inventory, player, playerName, value, value2)
         : this.shopsNavBar.load(inventory, player, playerName, value, value2);
   }

   public Inventory updateShopsNavBar(Inventory inventory, Player player, String playerName, int value, int value2) {
      return this.shopNavBars.containsKey(playerName)
         ? this.shopNavBars.get(playerName).update(inventory, player, playerName, value, value2)
         : this.shopsNavBar.update(inventory, player, playerName, value, value2);
   }

   public Inventory addMainMenuNavBar(Inventory inventory, Player player) {
      return this.mainMenuNavBar.load(inventory, player);
   }

   public ItemStack[] getMainMenuNavBar(Player player) {
      return this.mainMenuNavBar.get(player);
   }

   public String[] getMainMenuNavTextures() {
      return this.mainMenuNavBar.getBedrockTextures();
   }

   public Inventory addSellGUINavBar(Inventory inventory, Player player) {
      return this.sellGUINavBar.load(inventory, player);
   }

   public Inventory addTransactionNavBar(Inventory inventory, Transaction.Mode mode, Player player, ShopItem shopItem, EcoType ecoType) {
      return this.transactionNavBar.load(inventory, mode, player, shopItem, ecoType);
   }

   public Inventory addShopStandsNavBar(Inventory inventory, Transaction.Mode mode, Player player, ShopItem shopItem, EcoType ecoType) {
      return this.shopStandsNavBar.load(inventory, mode, player, shopItem, ecoType);
   }

   public Inventory addSearchNavBar(Inventory inventory, Player player, ShopSearch.QueryMethod queryMethod, int value, int value2) {
      return this.searchNavBar.load(inventory, player, queryMethod, value, value2);
   }

   public void createCustomPlayerProfile() {
      if (!ConfigManager.getConfig().getBoolean("custom-bedrock-player-profile.enabled", true)) {
         this.customPlayerProfile = null;
      } else {
         ConfigurationSection section = ConfigManager.getConfig().getConfigurationSection("custom-bedrock-player-profile.item");
         if (section != null && !section.getKeys(false).isEmpty()) {
            this.customPlayerProfile = this.createFillItem(section, null);
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

      public NavItem get(int value) {
         return this.items.get(value);
      }

      public InventoryNavBar(String text) {
         this.enabled = !ConfigManager.getSection(text).getString("nav-bar.mode", "INHERIT").equalsIgnoreCase("DISABLED");
         if (this.enabled) {
            LocalizedItemStack localizedItemStack = ConfigManager.getSection(text).contains("nav-bar.fill-item")
               ? NavBar.this.plugin.createItem.createFillItem(ConfigManager.getSection(text).getConfigurationSection("nav-bar.fill-item"), text)
               : null;
            this.fillItem = localizedItemStack == null ? null : localizedItemStack.getItem();
            ConfigurationSection section = ConfigManager.getSection(text).getConfigurationSection("nav-bar.items");
            if (section != null && !section.getKeys(false).isEmpty()) {
               int value = 1;

               for (String text2 : section.getKeys(false)) {
                  NavItem navItem = NavBar.this.getShopItem(text, "nav-bar.items." + text2, this.fillItem);
                  if (navItem == null) {
                     navItem = NavBar.this.shopsNavBar.get(value);
                  }

                  int value2 = 0;

                  try {
                     if (ConfigManager.getSection(text).contains("nav-bar.items." + text2 + ".slot")) {
                        value2 = ConfigManager.getSection(text).getInt("nav-bar.items." + text2 + ".slot");
                     } else {
                        try {
                           value2 = Integer.parseInt(text2);

                           while (this.items.containsKey(value2 - 1)) {
                              value2++;
                           }
                        } catch (NumberFormatException exception) {
                           value2 = value;

                           while (this.items.containsKey(value2 - 1)) {
                              value2++;
                           }
                        }
                     }
                  } catch (NumberFormatException exception) {
                     SendMessage.warnMessage(
                        "Cannot create navigation bar item for shop '"
                           + text
                           + "' at 'nav-bar.items."
                           + text2
                           + "' because the slot is not a valid integer. Got '"
                           + ConfigManager.getSection(text).getInt("nav-bar.items." + text2 + ".slot")
                           + "'"
                     );
                  }

                  if (value2 <= 9 && value2 >= 1) {
                     this.items.put(value2 - 1, navItem);
                     value++;
                  } else {
                     SendMessage.warnMessage(
                        "Cannot create navigation bar item for shop '"
                           + text
                           + "' at 'nav-bar.items."
                           + text2
                           + "' because the slot is out of bounds for "
                           + value2
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

      public InventoryNavBar(ConfigurationSection section) {
         this.enabled = section.getBoolean("enabled", true);
         if (this.enabled) {
            LocalizedItemStack localizedItemStack = section.contains("fill-item")
               ? NavBar.this.plugin.createItem.createFillItem(section.getConfigurationSection("fill-item"), null)
               : null;
            this.fillItem = localizedItemStack == null ? null : localizedItemStack.getItem();
            section = section.getConfigurationSection("items");
            if (section != null && !section.getKeys(false).isEmpty()) {
               int value = 1;

               for (String text : section.getKeys(false)) {
                  try {
                     NavItem navItem = NavBar.this.getDefaultItem(section.getCurrentPath() + "." + text, this.fillItem);
                     int value2 = 0;

                     try {
                        if (section.contains(text + ".slot")) {
                           value2 = section.getInt(text + ".slot");
                        } else {
                           try {
                              value2 = Integer.parseInt(text);

                              while (this.items.containsKey(value2 - 1)) {
                                 value2++;
                              }
                           } catch (NumberFormatException exception) {
                              value2 = value;

                              while (this.items.containsKey(value2 - 1)) {
                                 value2++;
                              }
                           }
                        }
                     } catch (NumberFormatException exception) {
                        SendMessage.warnMessage(
                           "Cannot create navigation bar item at '"
                              + section.getCurrentPath()
                              + "."
                              + text
                              + "' because the slot is not a valid integer. Got '"
                              + section.getString(text + ".slot")
                              + "'"
                        );
                     }

                     if (value2 <= 9 && value2 >= 1) {
                        this.items.put(value2 - 1, navItem);
                        value++;
                     } else {
                        SendMessage.warnMessage(
                           "Cannot create navigation bar item at '"
                              + section.getCurrentPath()
                              + "."
                              + text
                              + "' because the slot is out of bounds for "
                              + value2
                              + ". Valid values should be in range of 1-9"
                        );
                     }
                  } catch (ItemLoadException exception) {
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

      public Inventory load(Inventory inventory, Player player, String playerName, int value, int value2) {
         if (!this.enabled) {
            return inventory;
         } else {
            int size = inventory.getSize();

            for (int index = 0; index < 9; index++) {
               if (this.items.containsKey(index)) {
                  inventory.setItem(size - 9 + index, this.items.get(index).getItem(player, NavBar.this.plugin.getEcoHandler().getEcon(playerName).getType(), value, value2));
               } else if (this.fillItem != null) {
                  inventory.setItem(size - 9 + index, this.fillItem);
                  this.fillItems.add(index);
               } else {
                  inventory.setItem(size - 9 + index, new ItemStack(Material.AIR));
               }
            }

            return inventory;
         }
      }

      public Inventory update(Inventory inventory, Player player, String playerName, int value, int value2) {
         if (!this.enabled) {
            return inventory;
         } else {
            int size = inventory.getSize();

            for (int index = 0; index < 9; index++) {
               if (this.items.containsKey(index)) {
                  NavItem navItem = this.items.get(index);
                  if (!navItem.isStatic(value, value2)) {
                     inventory.setItem(size - 9 + index, navItem.getItem(player, NavBar.this.plugin.getEcoHandler().getEcon(playerName).getType(), value, value2));
                  }
               } else if (this.fillItem != null) {
                  if (!this.fillItems.contains(index)) {
                     inventory.setItem(size - 9 + index, this.fillItem);
                  }
               } else {
                  inventory.setItem(size - 9 + index, new ItemStack(Material.AIR));
               }
            }

            return inventory;
         }
      }

      public ItemStack[] get(Player player, String itemId, int value, int value2) {
         if (!this.enabled) {
            return null;
         } else {
            ItemStack[] itemStack = new ItemStack[9];

            for (Entry<Integer, NavItem> entry : this.items.entrySet()) {
               itemStack[entry.getKey()] = ((NavItem)entry.getValue()).getItem(player, NavBar.this.plugin.getEcoHandler().getEcon(itemId).getType(), value, value2);
            }

            if (this.fillItem != null) {
               for (int index = 0; index < 9; index++) {
                  if (!this.items.containsKey(index)) {
                     itemStack[index] = this.fillItem;
                  }
               }
            }

            return itemStack;
         }
      }

      public String[] getBedrockTextures(int value, int value2) {
         if (!this.enabled) {
            return null;
         } else {
            String[] text = new String[9];

            for (Entry<Integer, NavItem> entry : this.items.entrySet()) {
               text[entry.getKey()] = ((NavItem)entry.getValue()).getBedrockTexture(value, value2);
            }

            return text;
         }
      }

      public ItemStack[] get(Player player) {
         if (!this.enabled) {
            return null;
         } else {
            ItemStack[] itemStack = new ItemStack[9];

            for (Entry<Integer, NavItem> entry : this.items.entrySet()) {
               itemStack[entry.getKey()] = ((NavItem)entry.getValue()).getItem(player);
            }

            if (this.fillItem != null) {
               for (int index = 0; index < 9; index++) {
                  if (!this.items.containsKey(index)) {
                     itemStack[index] = this.fillItem;
                  }
               }
            }

            return itemStack;
         }
      }

      public String[] getBedrockTextures() {
         if (!this.enabled) {
            return null;
         } else {
            String[] text = new String[9];

            for (Entry<Integer, NavItem> entry : this.items.entrySet()) {
               text[entry.getKey()] = ((NavItem)entry.getValue()).getBedrockTexture();
            }

            return text;
         }
      }

      public Inventory load(Inventory inventory, Player player) {
         if (!this.enabled) {
            return inventory;
         } else {
            for (Entry entry : this.items.entrySet()) {
               inventory.setItem(inventory.getSize() - 9 + (Integer)entry.getKey(), ((NavItem)entry.getValue()).getItem(player));
            }

            if (this.fillItem != null) {
               for (int index = 0; index < 9; index++) {
                  if (!this.items.containsKey(index)) {
                     inventory.setItem(inventory.getSize() - 9 + index, this.fillItem);
                  }
               }
            }

            return inventory;
         }
      }

      public Inventory load(Inventory inventory, Transaction.Mode mode, Player player, ShopItem shopItem, EcoType ecoType) {
         if (!this.enabled) {
            return inventory;
         } else {
            for (Entry entry : this.items.entrySet()) {
               NavItem navItem = (NavItem)entry.getValue();
               if (!navItem.isDisabled()) {
                  inventory.setItem(inventory.getSize() - 9 + (Integer)entry.getKey(), navItem.getItem(player, mode, shopItem, ecoType));
               }
            }

            if (this.fillItem != null) {
               for (int index = 0; index < 9; index++) {
                  if (!this.items.containsKey(index)) {
                     inventory.setItem(inventory.getSize() - 9 + index, this.fillItem);
                  }
               }
            }

            return inventory;
         }
      }

      public Inventory load(Inventory inventory, Player player, ShopSearch.QueryMethod queryMethod, int value, int value2) {
         if (!this.enabled) {
            return inventory;
         } else {
            for (Entry entry : this.items.entrySet()) {
               inventory.setItem(inventory.getSize() - 9 + (Integer)entry.getKey(), ((NavItem)entry.getValue()).getItem(player, null, queryMethod, value, value2));
            }

            if (this.fillItem != null) {
               for (int index = 0; index < 9; index++) {
                  if (!this.items.containsKey(index)) {
                     inventory.setItem(inventory.getSize() - 9 + index, this.fillItem);
                  }
               }
            }

            return inventory;
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

      public static NavBar.ItemAction getFromString(String itemId) {
         try {
            return valueOf(itemId.toUpperCase(Locale.ENGLISH).replace(" ", "_"));
         } catch (IllegalArgumentException exception) {
            return NONE;
         }
      }
   }
}
