package com.onyxshop.events;

import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import com.onyxshop.OnyxShop;
import com.onyxshop.api.events.PostTransactionEvent;
import com.onyxshop.api.events.PreTransactionEvent;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.ItemEditor;
import com.onyxshop.inventorys.editor.items.AvailableActions;
import com.onyxshop.inventorys.editor.items.AvailableOptions;
import com.onyxshop.inventorys.editor.items.ChooseValue;
import com.onyxshop.inventorys.editor.items.ConclusionScreen;
import com.onyxshop.inventorys.editor.items.DeleteItemConfirmation;
import com.onyxshop.inventorys.editor.items.DynamicPricingOptions;
import com.onyxshop.inventorys.editor.items.FireworkOptions;
import com.onyxshop.inventorys.editor.items.IndexChooser;
import com.onyxshop.inventorys.editor.items.LimitedStockOptions;
import com.onyxshop.inventorys.editor.items.Main;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ActionItem;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.MainMenu;
import com.onyxshop.objects.SellGUI;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPage;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.objects.TransactionItem;
import com.onyxshop.objects.TransactionMenu;
import com.onyxshop.objects.TransactionMenus;
import com.onyxshop.objects.mappings.ClickAction;
import com.onyxshop.objects.shops.ShopSection;
import com.onyxshop.providers.UserManager;
import com.onyxshop.providers.economys.ItemEconomy;
import com.onyxshop.util.AnvilGUI;
import com.onyxshop.util.BackButton;
import com.onyxshop.util.CommandHelper;
import com.onyxshop.util.CommandType;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.EconomyType;
import com.onyxshop.util.FireworkUtil;
import com.onyxshop.util.InventoryUtil;
import com.onyxshop.util.PermissionsCache;
import com.onyxshop.util.PlayerItems;
import com.onyxshop.util.PotionTypes;
import com.onyxshop.util.Transaction;
import com.onyxshop.util.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class MenuHandler implements Listener {
   private final OnyxShop plugin;
   private final ConcurrentMap<UUID, Boolean> cooldown;
   public static final boolean backOnClose = ConfigManager.getConfig().getBoolean("escape-back");

   public MenuHandler(OnyxShop plugin) {
      this.plugin = plugin;
      int cooldown = Math.max(0, ConfigManager.getConfig().getInt("click-cooldown", 125));
      this.cooldown = (ConcurrentMap<UUID, Boolean>)(ConcurrentMap<?, ?>) CacheBuilder.newBuilder().expireAfterWrite(Duration.of(cooldown, ChronoUnit.MILLIS)).build().asMap();
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onMenuClick(InventoryClickEvent event) {
      int slot = event.getRawSlot();
      Inventory inventory = event.getInventory();
      InventoryHolder holder = InventoryUtil.getHolder(inventory);
      if (holder instanceof ShopInventory || holder instanceof ItemEditor) {
         if (slot >= inventory.getSize()) {
            if (event.getClick() != ClickType.SHIFT_LEFT && event.getClick() != ClickType.SHIFT_RIGHT) {
               if (event.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)) {
                  event.setCancelled(true);
               }
            } else if (holder instanceof SellGUI) {
               this.handleShiftSellGUI(event, inventory, holder, slot);
            } else {
               event.setCancelled(true);
            }
         } else {
            event.setCancelled(true);
            if (event.getClick() != ClickType.DOUBLE_CLICK) {
               Player player = (Player)event.getWhoClicked();
               if (this.cooldown.putIfAbsent(player.getUniqueId(), Boolean.TRUE) == null) {
                  if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR || holder instanceof SellGUI) {
                     if (holder instanceof ShopPage) {
                        ShopPage shopPage = (ShopPage)holder;
                        if (slot >= inventory.getSize() - 9 && slot <= inventory.getSize() - 1 && this.plugin.navBar.isEnabled(shopPage.getSection())) {
                           this.plugin.navBar.execute(player, shopPage, slot, shopPage.getPage(), shopPage.isDisabledBackButton());
                        } else {
                           DisplayItem displayItem = this.plugin.getDisplayItem(shopPage.getSection(), shopPage.getItem(slot));
                           if (displayItem == null) {
                              return;
                           }

                           ClickAction clickAction = this.plugin.getSection(shopPage.getSection()).getClickAction(event.getClick());
                           if (clickAction == null) {
                              return;
                           }

                           if (clickAction == ClickAction.GUI_EDITOR
                              && displayItem instanceof ShopItem
                              && PermissionsCache.hasPermission(player, "OnyxShop.editshop.editor")) {
                              new Main(player, (ShopItem)displayItem);
                           } else if (!displayItem.hasItemError()) {
                              if (!displayItem.canPurchase(player, "shop", false) || !this.isAllowedGamemode(player)) {
                                 return;
                              }

                              if (displayItem.meetsRequirements(player, false)) {
                                 if (displayItem.isClickCommand() && this.plugin.commandItems) {
                                    CommandHelper.executeCommandItem(displayItem, player, CommandType.CLICK);
                                 }

                                 if (displayItem instanceof ActionItem) {
                                    this.plugin.navBar.execute(player, shopPage, ((ActionItem)displayItem).getAction(), shopPage.getPage(), shopPage.isDisabledBackButton());
                                    return;
                                 }

                                 ShopItem shopItem = (ShopItem)displayItem;
                                 if (shopItem.isLinked()) {
                                    if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.getSubSection().toLowerCase())) {
                                       this.plugin.getSection(shopItem.getSubSection()).openShopSection(player, shopPage.isDisabledBackButton(), shopItem.section);
                                    } else {
                                       SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                                    }

                                    return;
                                 }

                                 if (shopItem.isDisplayItem()) {
                                    return;
                                 }

                                 if (clickAction == ClickAction.BUY) {
                                    if (!shopItem.isBuyAble()) {
                                       return;
                                    }

                                    if (shopItem.getMaxBuy() == -1) {
                                       if (shopItem.isABuyPricing()) {
                                          if (this.purchaseItemA(player, shopItem)) {
                                             inventory.setItem(event.getSlot(), this.plugin.getSection(shopItem.section).updateItem(player, shopItem));
                                          }
                                       } else if (this.purchaseItem(player, shopItem)) {
                                          inventory.setItem(event.getSlot(), this.plugin.getSection(shopItem.section).updateItem(player, shopItem));
                                       }
                                    } else {
                                       new TransactionMenu(
                                             player,
                                             shopItem,
                                             shopPage.getRootSection(),
                                             shopPage.isDisabledBackButton(),
                                             Transaction.Mode.BUY,
                                             Transaction.Type.BUY_SCREEN,
                                             1
                                          )
                                          .open(false);
                                    }
                                 } else if (clickAction == ClickAction.SELL_ALL) {
                                    if (!shopItem.isSellAble() || !this.plugin.MMB) {
                                       return;
                                    }

                                    if (shopItem.isASellPricing() ? this.sellItemA(player, shopItem, Integer.MAX_VALUE) : this.sellItem(player, shopItem, Integer.MAX_VALUE)) {
                                       inventory.setItem(event.getSlot(), this.plugin.getSection(shopItem.section).updateItem(player, shopItem));
                                    }
                                 } else if (clickAction == ClickAction.SELL) {
                                    if (!shopItem.isSellAble()) {
                                       return;
                                    }

                                    if (shopItem.getMaxSell() == -1) {
                                       if (shopItem.isASellPricing()) {
                                          if (this.sellItemA(player, shopItem, shopItem.getStackSize())) {
                                             inventory.setItem(event.getSlot(), this.plugin.getSection(shopItem.section).updateItem(player, shopItem));
                                          }
                                       } else if (this.sellItem(player, shopItem, shopItem.getStackSize())) {
                                          inventory.setItem(event.getSlot(), this.plugin.getSection(shopItem.section).updateItem(player, shopItem));
                                       }
                                    } else {
                                       new TransactionMenu(
                                             player,
                                             shopItem,
                                             shopPage.getRootSection(),
                                             shopPage.isDisabledBackButton(),
                                             Transaction.Mode.SELL,
                                             Transaction.Type.SELL_SCREEN,
                                             1
                                          )
                                          .open(false);
                                    }
                                 }
                              }
                           } else {
                              SendMessage.chatToPlayer(player, Lang.ITEM_ERROR.get());
                           }
                        }
                     } else if (holder instanceof TransactionMenu) {
                        TransactionMenu transactionMenu = (TransactionMenu)holder;
                        if (slot >= inventory.getSize() - 9 && slot <= inventory.getSize() - 1) {
                           switch (transactionMenu.getTransactionType()) {
                              case SHOPSTAND_BUY_SCREEN:
                              case SHOPSTAND_SELL_SCREEN:
                                 if (this.plugin.navBar.isEnableShopStandsNav()) {
                                    this.plugin
                                       .navBar
                                       .execute(transactionMenu, player, transactionMenu.getShopItem(), slot - (inventory.getSize() - 9), transactionMenu.getTransactionMode(), transactionMenu.getAmount());
                                 }
                                 break;
                              default:
                                 if (this.plugin.navBar.isEnableTransactionNav()) {
                                    this.plugin
                                       .navBar
                                       .execute(
                                          transactionMenu,
                                          player,
                                          transactionMenu.getShopItem(),
                                          slot - (inventory.getSize() - 9),
                                          transactionMenu.getRootSection(),
                                          transactionMenu.isDisabledBackButton(),
                                          transactionMenu.getTransactionMode(),
                                          transactionMenu.getTransactionType(),
                                          transactionMenu.getAmount()
                                       );
                                 }
                           }
                        }

                        TransactionItem transactionItem = TransactionMenus.getItemFromSlot(transactionMenu.getTransactionType(), slot);
                        if (transactionItem != null) {
                           switch (transactionItem.getAction().type) {
                              case NONE:
                                 break;
                              case BACK:
                                 if (transactionMenu.getTransactionType() != Transaction.Type.BUY_STACKS_SCREEN) {
                                    ShopSection section = this.plugin.getSection(transactionMenu.getShopItem().section);
                                    section.openShopSection(
                                       player, section.getPageForShopItem(transactionMenu.getShopItem().itemLoc), transactionMenu.isDisabledBackButton(), transactionMenu.getRootSection()
                                    );
                                    this.plugin.getSoundEffects().useBackButton(player);
                                 } else {
                                    new TransactionMenu(
                                          player,
                                          transactionMenu.getShopItem(),
                                          transactionMenu.getRootSection(),
                                          transactionMenu.isDisabledBackButton(),
                                          transactionMenu.getTransactionMode(),
                                          Transaction.Type.BUY_SCREEN,
                                          transactionMenu.getAmount()
                                       )
                                       .open(false);
                                 }
                                 break;
                              case COMMAND:
                                 List<String> clickCommands = transactionItem.getClickCommands();
                                 if (!clickCommands.isEmpty()) {
                                    clickCommands.forEach(entry -> CommandHelper.executeCommand(null, player, entry));
                                 }
                                 break;
                              case INSTA_BUY:
                              case INSTA_SELL:
                                 transactionMenu.setAmount(transactionItem.getItem().getAmount());
                              case CONFIRM_TRANSACTION:
                                 if (event.getClick().isLeftClick()) {
                                    if (transactionMenu.isAPricing()) {
                                       this.completeTransactionA(transactionMenu, player);
                                    } else {
                                       this.completeTransaction(transactionMenu, player);
                                    }
                                 } else if (event.getClick().isRightClick() && transactionMenu.isAPricing() && !transactionMenu.isAllPrices()) {
                                    transactionMenu.cycleEcoType();
                                 }
                                 break;
                              case OPEN_BUY_STACKS:
                                 if (!transactionMenu.getShopItem().isMaxBuy(transactionMenu.getMaxStackSize()) && transactionMenu.getShopItem().isAllowQuantity()) {
                                    new TransactionMenu(
                                          player,
                                          transactionMenu.getShopItem(),
                                          transactionMenu.getRootSection(),
                                          transactionMenu.isDisabledBackButton(),
                                          Transaction.Mode.BUY,
                                          Transaction.Type.BUY_STACKS_SCREEN,
                                          transactionMenu.getAmount()
                                       )
                                       .open(false);
                                 }
                                 break;
                              case SELL_ALL:
                                 this.completeSellAll(transactionMenu, player);
                                 break;
                              default:
                                 if (transactionMenu.getShopItem().isAllowQuantity()) {
                                    transactionMenu.updateAmount(transactionItem.getAction());
                                    if (event.getCurrentItem() != null && transactionItem.getItem().getType() == event.getCurrentItem().getType()) {
                                       this.plugin.getSoundEffects().changeItemAmount(player);
                                    }
                                 }
                           }
                        }
                     } else if (holder instanceof MainMenu) {
                        if (this.plugin.navBar.isEnableMainNav() && slot >= inventory.getSize() - 9 && slot <= inventory.getSize() - 1) {
                           this.plugin.navBar.execute(holder, player, slot - (inventory.getSize() - 9));
                        } else {
                           String mainMenuSectionForSlot = this.plugin.getMainMenuSectionForSlot(slot);
                           if (mainMenuSectionForSlot != null && this.plugin.getSections().containsKey(mainMenuSectionForSlot)) {
                              if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + mainMenuSectionForSlot.toLowerCase())) {
                                 ShopSection section = this.plugin.getSection(mainMenuSectionForSlot);
                                 if (!section.isCommandItem()) {
                                    section.openShopSection(player, false);
                                 } else {
                                    section.getClickCommands().forEach(entry -> CommandHelper.executeCommand(null, player, entry));
                                    if (section.isCloseMenu()) {
                                       this.close(player);
                                    }
                                 }

                                 this.plugin.getSoundEffects().openShop(player);
                              } else if (!this.plugin.getSectionItem(mainMenuSectionForSlot).isHideIfNoPerms()) {
                                 SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                              }
                           }
                        }
                     } else if (holder instanceof ShopSearch) {
                        ShopSearch shopSearch = (ShopSearch)holder;
                        if (this.plugin.navBar.isEnableSearchNav() && slot >= inventory.getSize() - 9 && slot <= inventory.getSize() - 1) {
                           this.plugin.navBar.execute(shopSearch, player, slot - (inventory.getSize() - 9), shopSearch.getPage());
                        } else {
                           ShopItem shopItem = shopSearch.getShopItemForSlot(slot);
                           if (event.getClick().isShiftClick() && PermissionsCache.hasPermission(player, "OnyxShop.editshop.editor")) {
                              new Main(player, shopItem);
                           } else if (!shopItem.hasItemError()) {
                              if (!shopItem.canPurchase(player, "shop", false) || !this.isAllowedGamemode(player)) {
                                 return;
                              }

                              if (PermissionsCache.hasPermission(player, "OnyxShop.shop." + shopItem.section)) {
                                 if (event.getClick().isLeftClick()) {
                                    if (shopItem.isBuyAble()) {
                                       new TransactionMenu(player, shopItem, null, true, Transaction.Mode.BUY, Transaction.Type.BUY_SCREEN, 1).open(false);
                                    }
                                 } else if (event.getClick().isRightClick() && shopItem.isSellAble()) {
                                    new TransactionMenu(player, shopItem, null, true, Transaction.Mode.SELL, Transaction.Type.SELL_SCREEN, 1).open(false);
                                 }
                              } else {
                                 SendMessage.chatToPlayer(player, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                              }
                           } else {
                              SendMessage.chatToPlayer(player, Lang.ITEM_ERROR.get());
                           }
                        }
                     } else if (holder instanceof SellGUI) {
                        if (this.plugin.navBar.isEnableSellGUINav() && slot >= inventory.getSize() - 9 && slot <= inventory.getSize() - 1) {
                           this.plugin.navBar.execute(holder, player, slot - (inventory.getSize() - 9));
                        } else if (!SellGUI.isFillItem(slot)) {
                           event.setCancelled(false);
                        }
                     } else if (holder instanceof ItemEditor) {
                        this.handleItemEditor(event, player, inventory, holder, slot);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleItemEditor(InventoryClickEvent inventoryClickEvent, Player player, Inventory inventory, InventoryHolder holder, int value) {
      if (holder instanceof Main) {
         Main main = (Main)holder;
         switch (value) {
            case 29:
               this.plugin.itemEditor.openItemEditorCreateItem(player, main.getItem(), null, null, null);
               break;
            case 31:
               new AvailableOptions(player, main.getItem());
               break;
            case 33:
               new DeleteItemConfirmation(player, main.getItem());
               break;
            case 53:
               this.plugin
                  .getSection(main.getItem().section)
                  .openShopSection(player, this.plugin.getSection(main.getItem().section).getPageForShopItem(main.getItem().itemLoc), false);
         }
      } else if (holder instanceof IndexChooser) {
         IndexChooser indexChooser = (IndexChooser)holder;
         Integer onyxNbtInt = this.plugin.versionHandler.getOnyxNbtInt(inventoryClickEvent.getCurrentItem(), "Index");
         if (onyxNbtInt != null) {
            this.plugin.itemEditor.openAvailableResultsForOption(player, indexChooser.getOption(), indexChooser.getAction(), indexChooser.getItem(), onyxNbtInt);
         } else if (value == inventory.getSize() - 1) {
            new AvailableActions(player, indexChooser.getItem(), indexChooser.getOption());
         }
      } else if (holder instanceof FireworkOptions) {
         FireworkOptions fireworkOptions = (FireworkOptions)holder;
         com.onyxshop.methodes.ItemEditor.Option option = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(inventoryClickEvent.getCurrentItem())
         );
         if (option != null) {
            if (option != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_SHAPE
               && option != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FLICKER
               && option != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_TRAIL
               && option != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_DURATION) {
               new AvailableActions(player, fireworkOptions.getItem(), option);
            } else {
               inventory.setItem(value, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(inventoryClickEvent.getCurrentItem(), "value"), option));
            }
         } else if (value == 17) {
            new AvailableOptions(player, fireworkOptions.getItem());
         }
      } else if (holder instanceof DynamicPricingOptions) {
         DynamicPricingOptions dynamicPricingOptions = (DynamicPricingOptions)holder;
         com.onyxshop.methodes.ItemEditor.Option option = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(inventoryClickEvent.getCurrentItem())
         );
         if (option != null) {
            if (option == com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_ENABLE) {
               inventory.setItem(value, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(inventoryClickEvent.getCurrentItem(), "value"), option));
            } else {
               new AvailableActions(player, dynamicPricingOptions.getItem(), option);
            }
         } else if (value == 17) {
            new AvailableOptions(player, dynamicPricingOptions.getItem());
         }
      } else if (holder instanceof LimitedStockOptions) {
         LimitedStockOptions limitedStockOptions = (LimitedStockOptions)holder;
         com.onyxshop.methodes.ItemEditor.Option option = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(inventoryClickEvent.getCurrentItem())
         );
         if (option != null) {
            new AvailableActions(player, limitedStockOptions.getItem(), option);
         } else if (value == 17) {
            new AvailableOptions(player, limitedStockOptions.getItem());
         }
      } else if (holder instanceof DeleteItemConfirmation) {
         DeleteItemConfirmation deleteItemConfirmation = (DeleteItemConfirmation)holder;
         switch (value) {
            case 3:
               ConfigManager.getShop(deleteItemConfirmation.getItem().section).set("pages." + deleteItemConfirmation.getItem().itemLoc, null);
               ConfigManager.saveShop(deleteItemConfirmation.getItem().section);
               new ConclusionScreen(player, deleteItemConfirmation.getItem(), null, com.onyxshop.methodes.ItemEditor.Action.REMOVE_ITEM, null);
            case 5:
            default:
               break;
            case 17:
               new Main(player, deleteItemConfirmation.getItem());
         }
      } else if (holder instanceof ConclusionScreen) {
         ConclusionScreen conclusionScreen = (ConclusionScreen)holder;
         if (value == 17) {
            switch (conclusionScreen.getAction()) {
               case REMOVE_ITEM:
                  this.plugin.getSection(conclusionScreen.getItem().section).openShopSection(player, false);
                  break;
               case REMOVE:
               case EDIT:
               case ADD:
                  new Main(player, this.plugin.getShopItem(conclusionScreen.getItem().section, conclusionScreen.getItem().itemLoc));
                  break;
               case ADD_ITEM:
                  new Main(player, this.plugin.getShopItem(conclusionScreen.getNewItem()));
            }
         }
      } else if (holder instanceof AvailableOptions) {
         AvailableOptions availableOptions = (AvailableOptions)holder;
         com.onyxshop.methodes.ItemEditor.Option option = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(inventoryClickEvent.getCurrentItem())
         );
         if (option != null) {
            switch (option) {
               case ENCHANTMENT_GLINT:
                  inventory.setItem(value, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(inventoryClickEvent.getCurrentItem(), "value"), option));
                  break;
               case DYNAMIC_PRICING:
                  new DynamicPricingOptions(player, availableOptions.getItem());
                  break;
               case FIREWORK:
                  new FireworkOptions(player, availableOptions.getItem());
                  break;
               case GLOBAL_STOCK:
               case PLAYER_STOCK:
                  new LimitedStockOptions(
                     player,
                     availableOptions.getItem(),
                     option == com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK
                        ? LimitedStockOptions.Type.GLOBAL
                        : LimitedStockOptions.Type.PLAYER
                  );
                  break;
               default:
                  new AvailableActions(player, availableOptions.getItem(), option);
            }
         } else if (value == 53) {
            new Main(player, availableOptions.getItem());
         }
      } else if (holder instanceof AvailableActions) {
         AvailableActions availableActions = (AvailableActions)holder;
         String action = this.plugin.versionHandler.getAction(inventoryClickEvent.getCurrentItem());
         if (action != null) {
            this.plugin
               .itemEditor
               .openAvailableResultsForOption(
                  player, availableActions.getOption(), com.onyxshop.methodes.ItemEditor.Action.valueOf(action.toUpperCase(Locale.ENGLISH)), availableActions.getItem(), -1
               );
         } else if (value == 17) {
            switch (availableActions.getOption()) {
               case FIREWORK_SHAPE:
               case FIREWORK_FLICKER:
               case FIREWORK_TRAIL:
               case FIREWORK_DURATION:
               case FIREWORK_COLORS:
               case FIREWORK_FADE_COLORS:
                  new FireworkOptions(player, availableActions.getItem());
                  break;
               case DYNAMIC_PRICING_ENABLE:
               case DYNAMIC_PRICING_MAX_STOCK:
                  new DynamicPricingOptions(player, availableActions.getItem());
                  break;
               case GLOBAL_STOCK_LIMIT:
               case GLOBAL_STOCK_TIMER:
               case GLOBAL_STOCK_FREQUENCY:
                  new LimitedStockOptions(player, availableActions.getItem(), LimitedStockOptions.Type.GLOBAL);
                  break;
               case PLAYER_STOCK_LIMIT:
               case PLAYER_STOCK_TIMER:
               case PLAYER_STOCK_FREQUENCY:
                  new LimitedStockOptions(player, availableActions.getItem(), LimitedStockOptions.Type.PLAYER);
                  break;
               default:
                  new AvailableOptions(player, availableActions.getItem());
            }
         }
      } else if (holder instanceof ChooseValue) {
         ChooseValue chooseValue = (ChooseValue)holder;
         if (value == inventory.getSize() - 1) {
            if (chooseValue.getAction() != com.onyxshop.methodes.ItemEditor.Action.ADD) {
               new AvailableActions(player, chooseValue.getItem(), chooseValue.getOption());
            } else {
               new Main(player, chooseValue.getItem());
            }
         } else if (value == inventory.getSize() - 6) {
            if (chooseValue.getPage() > 1) {
               new ChooseValue(chooseValue, player, chooseValue.getPage() - 1);
            }
         } else if (value == inventory.getSize() - 4) {
            if (chooseValue.getPage() != chooseValue.getAllPages()) {
               new ChooseValue(chooseValue, player, chooseValue.getPage() + 1);
            }
         } else if (value == inventory.getSize() - 5) {
            new AnvilGUI.Builder()
               .onComplete(
                  (player2, input) -> {
                     this.plugin
                        .runTaskLaterFor(
                           () -> new ChooseValue(
                              player, chooseValue.getItem(), chooseValue.getOption(), chooseValue.getAction(), chooseValue.getIndex(), 1, input.toLowerCase(Locale.ROOT)
                           ),
                           player,
                           1L
                        );
                     return AnvilGUI.Response.close();
                  }
               )
               .onClose(
                  player2 -> this.plugin
                     .runTaskLaterFor(
                        () -> new ChooseValue(player, chooseValue.getItem(), chooseValue.getOption(), chooseValue.getAction(), chooseValue.getIndex(), chooseValue.getPage()), player, 1L
                     )
               )
               .text("Search text")
               .itemLeft(new ItemStack(Material.PAPER))
               .title(Lang.ANVILGUI_ENTER_VALUE.get().getLegacy().replace("%option%", chooseValue.getOption().getName()))
               .plugin(this.plugin)
               .open(player);
         } else {
            String itemId = null;
            switch (chooseValue.getOption()) {
               case FIREWORK_COLORS:
               case FIREWORK_FADE_COLORS:
                  itemId = FireworkUtil.FireworkColor.getFromID(XMaterial.matchXMaterial(inventoryClickEvent.getCurrentItem()).getData()).name();
               case DYNAMIC_PRICING_ENABLE:
               case DYNAMIC_PRICING_MAX_STOCK:
               case GLOBAL_STOCK_LIMIT:
               case GLOBAL_STOCK_TIMER:
               case GLOBAL_STOCK_FREQUENCY:
               case PLAYER_STOCK_LIMIT:
               case PLAYER_STOCK_TIMER:
               case PLAYER_STOCK_FREQUENCY:
               default:
                  break;
               case MATERIAL:
               case RECIPES:
                  itemId = inventoryClickEvent.getCurrentItem().getType().name();
                  if (chooseValue.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD) {
                     this.plugin.itemEditor.openItemEditorCreateItem(player, chooseValue.getItem(), itemId, null, null);
                     return;
                  }
                  break;
               case ENCHANTMENTS:
                  new AnvilGUI.Builder()
                     .onComplete(
                        (player2, input) -> {
                           try {
                              String text = this.plugin.enchantsProvider.getEnchants(inventoryClickEvent.getCurrentItem()).get(0) + ":" + Integer.parseInt(input);
                              if (chooseValue.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD && chooseValue.getIndex() == -1) {
                                 List stringList = ConfigManager.getShop(chooseValue.getItem().section)
                                    .getStringList("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey());
                                 stringList.add(text);
                                 ConfigManager.getShop(chooseValue.getItem().section)
                                    .set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), stringList);
                              } else {
                                 List stringList = ConfigManager.getShop(chooseValue.getItem().section)
                                    .getStringList("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey());
                                 if (chooseValue.getIndex() != -1) {
                                    stringList.set(chooseValue.getIndex(), text);
                                 } else {
                                    stringList.clear();
                                    stringList.add(text);
                                 }

                                 ConfigManager.getShop(chooseValue.getItem().section)
                                    .set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), stringList);
                              }

                              ConfigManager.saveShop(chooseValue.getItem().section);
                              this.plugin
                                 .runTaskLaterFor(() -> new ConclusionScreen(player, chooseValue.getItem(), chooseValue.getOption(), chooseValue.getAction(), null), player, 1L);
                              return AnvilGUI.Response.close();
                           } catch (NumberFormatException exception) {
                              return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                           }
                        }
                     )
                     .onClose(player2 -> {
                        if (chooseValue.getIndex() != -1) {
                           this.plugin.runTaskLaterFor(() -> new IndexChooser(player, chooseValue.getItem(), chooseValue.getOption(), chooseValue.getAction()), player, 1L);
                        } else {
                           this.plugin.runTaskLaterFor(() -> new AvailableActions(player, chooseValue.getItem(), chooseValue.getOption()), player, 1L);
                        }
                     })
                     .text(Lang.OPTION_STRENGTH.get().getLegacy())
                     .itemLeft(new ItemStack(Material.PAPER))
                     .title(Lang.ANVILGUI_ENTER_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_STRENGTH.get().getLegacy()))
                     .plugin(this.plugin)
                     .open(player);
                  return;
               case POTIONTYPES:
                  itemId = PotionTypes.getPotionTypes(inventoryClickEvent.getCurrentItem()).get(0).toString();
                  break;
               case SPAWNERTYPE:
                  itemId = inventoryClickEvent.getCurrentItem().getItemMeta().getDisplayName();
                  break;
               case SUB_SECTION:
                  itemId = this.plugin.versionHandler.getNBTString(inventoryClickEvent.getCurrentItem(), "section");
            }

            if (chooseValue.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD && chooseValue.getIndex() == -1) {
               if (this.plugin.itemEditor.allListKeys.contains(chooseValue.getOption().getKey())) {
                  List stringList = ConfigManager.getShop(chooseValue.getItem().section)
                     .getStringList("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey());
                  stringList.add(itemId);
                  ConfigManager.getShop(chooseValue.getItem().section).set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), stringList);
               } else {
                  ConfigManager.getShop(chooseValue.getItem().section).set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), itemId);
               }
            } else if (chooseValue.getAction() == com.onyxshop.methodes.ItemEditor.Action.EDIT) {
               if (this.plugin.itemEditor.allListKeys.contains(chooseValue.getOption().getKey())) {
                  List stringList = ConfigManager.getShop(chooseValue.getItem().section)
                     .getStringList("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey());
                  if (chooseValue.getIndex() != -1) {
                     stringList.set(chooseValue.getIndex(), itemId);
                  } else {
                     stringList.clear();
                     stringList.add(itemId);
                  }

                  ConfigManager.getShop(chooseValue.getItem().section).set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), stringList);
               } else {
                  ConfigManager.getShop(chooseValue.getItem().section).set("pages." + chooseValue.getItem().itemLoc + "." + chooseValue.getOption().getKey(), itemId);
               }
            }

            ConfigManager.saveShop(chooseValue.getItem().section);
            new ConclusionScreen(player, chooseValue.getItem(), chooseValue.getOption(), chooseValue.getAction(), null);
         }
      }
   }

   public void completeTransaction(TransactionMenu transactionMenu, Player player) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      double amount = 0.0;
      int value = 0;
      if (transactionMenu.getTransactionMode() != Transaction.Mode.SELL) {
         if (transactionMenu.isPurchaseAble()) {
            int freeSpace = transactionMenu.getFreeSpace();
            if (freeSpace != 0) {
               value = transactionMenu.getAmountToSell(freeSpace);
               if (transactionMenu.getShopItem().getLimitedStockMode() != 0 && transactionMenu.getItemStock() <= 0) {
                  SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(player);
                  result = Transaction.Result.NO_ITEM_STOCK_LEFT;
                  if (transactionMenu.getItemStock() == -1 || transactionMenu.getItemStock() == -2) {
                     transactionMenu.setSelectedItem(true);
                  }
               } else {
                  PreTransactionEvent preTransactionEvent = new PreTransactionEvent(transactionMenu.getShopItem(), player, value, transactionMenu.getPrice(value), transactionMenu.getTransactionType());
                  Bukkit.getPluginManager().callEvent(preTransactionEvent);
                  amount = preTransactionEvent.getPrice();
                  if (!preTransactionEvent.isCancelled()) {
                     if (!(amount >= 0.0)) {
                        SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
                        result = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     } else if (this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).getBalance(player) >= amount) {
                        if (!this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).withdrawBalance(player, amount)) {
                           SendMessage.chatToPlayer(
                              player,
                              Lang.INSUFFICIENT_MONEY
                                 .get()
                                 .replace("%currency%", this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).getFriendly())
                           );
                           this.plugin.getSoundEffects().insufficientFunds(player);
                           result = Transaction.Result.INSUFFICIENT_FUNDS;
                        } else {
                           if (transactionMenu.getShopItem().getLimitedStockMode() != 0) {
                              transactionMenu.updateItemStock(value);
                              this.plugin.getLimitedStockProvider().buyItems(transactionMenu.getShopItem(), player.getUniqueId(), value);
                           }

                           if (transactionMenu.getShopItem().isBuyCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(transactionMenu.getShopItem(), player, CommandType.BUY, String.valueOf(amount), value);
                              result = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           if (!transactionMenu.getShopItem().isBuyCommand() || transactionMenu.getShopItem().isGiveItem()) {
                              int undelivered = PlayerItems.giveItems(player, transactionMenu.getShopItem(), value);
                              if (undelivered > 0 && value > 0) {
                                 this.plugin
                                    .getEcoHandler()
                                    .getEcon(transactionMenu.getShopItem().getEcoType())
                                    .depositBalance(player, amount / value * undelivered);
                                 if (transactionMenu.getShopItem().getLimitedStockMode() != 0) {
                                    transactionMenu.addItemStock(undelivered);
                                    this.plugin.getLimitedStockProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), undelivered);
                                 }

                                 SendMessage.chatToPlayer(
                                    player,
                                    Lang.COULD_NOT_STORE_ALL_ITEMS.get().replace("%amount%", Integer.toString(undelivered))
                                 );
                              }
                           }

                           if (this.plugin.dynamicPricing && transactionMenu.getShopItem().isDynamicPricing()) {
                              this.plugin.getDynamicPricingProvider().buyItem(transactionMenu.getItemPath(), value);
                           }

                           transactionMenu.sendTransactionCompleteMessage(amount, value);
                           if (transactionMenu.getShopItem().isCloseMenu()) {
                              this.close(player);
                           } else {
                              transactionMenu.open(true);
                           }
                        }
                     } else {
                        SendMessage.chatToPlayer(
                           player,
                           Lang.INSUFFICIENT_MONEY
                              .get()
                              .replace("%currency%", this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).getFriendly())
                        );
                        this.plugin.getSoundEffects().insufficientFunds(player);
                        result = Transaction.Result.INSUFFICIENT_FUNDS;
                     }
                  } else {
                     result = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(player, Lang.MORE_SPACE_NEEDED.get());
               result = Transaction.Result.NO_INVENTORY_SPACE;
            }
         } else {
            SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
            result = Transaction.Result.NEGATIVE_ITEM_PRICE;
         }
      } else {
         synchronized (this.getSellLock()) {
         Double availableItems = transactionMenu.getAvailableItems(transactionMenu.getAmount());
         if (availableItems != null) {
            if (availableItems >= 0.0) {
               if (transactionMenu.getShopItem().getEcoType().getType() == EconomyType.ITEM && !this.canPay(player, transactionMenu.getShopItem().getEcoType(), availableItems)) {
                  SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get());
                  transactionMenu.emptyCache();
                  result = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  PreTransactionEvent preTransactionEvent = new PreTransactionEvent(transactionMenu.getShopItem(), player, value = transactionMenu.getAmount(), availableItems, transactionMenu.getTransactionType());
                  Bukkit.getPluginManager().callEvent(preTransactionEvent);
                  amount = preTransactionEvent.getPrice();
                  if (!preTransactionEvent.isCancelled()) {
                     if (amount >= 0.0) {
                        final int sold = transactionMenu.getAmount();
                        final List<ItemStack> removedStacks = transactionMenu.takeCachedItems();
                        boolean flag = false;
                        try {
                           this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).depositBalance(player, amount);
                           flag = true;
                           if (transactionMenu.getShopItem().isSellCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(transactionMenu.getShopItem(), player, CommandType.SELL, String.valueOf(amount), sold);
                              result = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           transactionMenu.sendTransactionCompleteMessage(amount);
                           this.plugin.runTaskAsync(() -> this.sellItems(transactionMenu.getShopItem(), sold, player.getUniqueId()));
                           if (transactionMenu.getShopItem().getLimitedSellMode() != 0) {
                              transactionMenu.updateSellLimit(sold);
                              this.plugin.getLimitedSellProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold, false);
                           }

                           if (transactionMenu.getShopItem().isRefillStock()) {
                              transactionMenu.addItemStock(sold);
                              this.plugin.getLimitedStockProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold);
                           }

                           if (transactionMenu.getShopItem().isCloseMenu()) {
                              this.close(player);
                           } else {
                              transactionMenu.open(true);
                           }
                        } catch (RuntimeException exception) {
                           if (flag) {
                              SendMessage.errorMessage("Sell completed but post-processing failed: " + exception.getMessage());
                           } else {
                              player.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                              SendMessage.errorMessage("Rolled back a failed sell: " + exception.getMessage());
                              result = Transaction.Result.TRANSACTION_CANCELLED;
                           }
                        }
                     } else {
                        transactionMenu.emptyCache();
                        result = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     }
                  } else {
                     transactionMenu.emptyCache();
                     result = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.plugin.getSoundEffects().insufficientStock(player);
               transactionMenu.emptyCache();
               result = Transaction.Result.REACHED_SELL_LIMIT;
               if (transactionMenu.getSellLimit() == -1 || transactionMenu.getSellLimit() == -2) {
                  transactionMenu.setSelectedItem(true);
               }
            }
         } else {
            SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get());
            transactionMenu.emptyCache();
            result = Transaction.Result.NO_ITEMS_FOUND;
         }
      }

      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(transactionMenu.getShopItem(), player, value, amount, transactionMenu.getTransactionType(), result));
   }

   public void completeTransactionA(TransactionMenu transactionMenu, Player player) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      Map<EcoType, Double> map = new HashMap<>();
      int value = 0;
      if (transactionMenu.getTransactionMode() != Transaction.Mode.SELL) {
         if (transactionMenu.isPurchaseAble()) {
            int freeSpace = transactionMenu.getFreeSpace();
            if (freeSpace != 0) {
               value = transactionMenu.getAmountToSell(freeSpace);
               if (transactionMenu.getShopItem().getLimitedStockMode() != 0 && transactionMenu.getItemStock() <= 0) {
                  SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(player);
                  result = Transaction.Result.NO_ITEM_STOCK_LEFT;
                  if (transactionMenu.getItemStock() == -1 || transactionMenu.getItemStock() == -2) {
                     transactionMenu.setSelectedItem(true);
                  }
               } else {
                  PreTransactionEvent preTransactionEvent = new PreTransactionEvent(
                     Collections.singletonMap(transactionMenu.getShopItem(), value), transactionMenu.getPrices(value), player, value, transactionMenu.getTransactionType()
                  );
                  Bukkit.getPluginManager().callEvent(preTransactionEvent);
                  map = preTransactionEvent.getPrices();
                  if (!preTransactionEvent.isCancelled()) {
                     Map types = this.plugin.getEcoHandler().has((Map<EcoType, Double>)map, player);
                     if (!this.arePricesValid((Map<EcoType, Double>)map)) {
                        SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
                        result = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     } else if (types.size() == 0) {
                        if (!this.plugin.getEcoHandler().withdraw((Map<EcoType, Double>)map, player)) {
                           SendMessage.chatToPlayer(
                              player,
                              Lang.INSUFFICIENT_MONEY
                                 .get()
                                 .replace(
                                    "%currency%",
                                    this.plugin.formatPrices(
                                       Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0],
                                       map.keySet(),
                                       transactionMenu.getShopItem().getABuyPrice().isAll()
                                    )
                                 )
                           );
                           this.plugin.getSoundEffects().insufficientFunds(player);
                           result = Transaction.Result.INSUFFICIENT_FUNDS;
                        } else {
                           if (transactionMenu.getShopItem().getLimitedStockMode() != 0) {
                              transactionMenu.updateItemStock(value);
                              this.plugin.getLimitedStockProvider().buyItems(transactionMenu.getShopItem(), player.getUniqueId(), value);
                           }

                           if (transactionMenu.getShopItem().isBuyCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(transactionMenu.getShopItem(), player, CommandType.BUY, map.toString(), value);
                              result = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           if (!transactionMenu.getShopItem().isBuyCommand() || transactionMenu.getShopItem().isGiveItem()) {
                              PlayerItems.giveItems(player, transactionMenu.getShopItem(), value);
                           }

                           if (this.plugin.dynamicPricing && transactionMenu.getShopItem().isDynamicPricing()) {
                              this.plugin.getDynamicPricingProvider().buyItem(transactionMenu.getItemPath(), value);
                           }

                           transactionMenu.sendTransactionCompleteMessage((Map<EcoType, Double>)map, value);
                           if (transactionMenu.getShopItem().isCloseMenu()) {
                              this.close(player);
                           } else {
                              transactionMenu.open(true);
                           }
                        }
                     } else {
                        SendMessage.chatToPlayer(
                           player,
                           Lang.INSUFFICIENT_MONEY
                              .get()
                              .replace(
                                 "%currency%",
                                 this.plugin
                                    .formatPrices(
                                       Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0],
                                       types.keySet(),
                                       transactionMenu.getShopItem().getABuyPrice().isAll()
                                    )
                              )
                        );
                        this.plugin.getSoundEffects().insufficientFunds(player);
                        result = Transaction.Result.INSUFFICIENT_FUNDS;
                     }
                  } else {
                     result = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(player, Lang.MORE_SPACE_NEEDED.get());
               result = Transaction.Result.NO_INVENTORY_SPACE;
            }
         } else {
            SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
            result = Transaction.Result.NEGATIVE_ITEM_PRICE;
         }
      } else {
         synchronized (this.getSellLock()) {
         value = transactionMenu.getAmount();
         map = transactionMenu.getAvailableItemsA(transactionMenu.getAmount());
         if (map != null) {
            if (!map.isEmpty()) {
               if (!map.keySet().stream().noneMatch(element -> element.getType() == EconomyType.ITEM) && !this.canPay((Map<EcoType, Double>)map, player)) {
                  SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get());
                  transactionMenu.emptyCache();
                  result = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  PreTransactionEvent preTransactionEvent = new PreTransactionEvent(
                     Collections.singletonMap(transactionMenu.getShopItem(), value), (Map<EcoType, Double>)map, player, value, transactionMenu.getTransactionType()
                  );
                  Bukkit.getPluginManager().callEvent(preTransactionEvent);
                  map = preTransactionEvent.getPrices();
                  if (!preTransactionEvent.isCancelled()) {
                     final int sold = transactionMenu.getAmount();
                     final List<ItemStack> removedStacks = transactionMenu.takeCachedItems();
                     boolean flag = false;
                     try {
                        this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)map, player);
                        flag = true;
                        if (transactionMenu.getShopItem().isSellCommand() && this.plugin.commandItems) {
                           CommandHelper.executeCommandItem(transactionMenu.getShopItem(), player, CommandType.SELL, map.toString(), sold);
                           result = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                        }

                        transactionMenu.sendTransactionCompleteMessage((Map<EcoType, Double>)map, sold);
                        this.plugin.runTaskAsync(() -> this.sellItems(transactionMenu.getShopItem(), sold, player.getUniqueId()));
                        if (transactionMenu.getShopItem().getLimitedSellMode() != 0) {
                           transactionMenu.updateSellLimit(sold);
                           this.plugin.getLimitedSellProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold, false);
                        }

                        if (transactionMenu.getShopItem().isRefillStock()) {
                           transactionMenu.addItemStock(sold);
                           this.plugin.getLimitedStockProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold);
                        }

                        if (transactionMenu.getShopItem().isCloseMenu()) {
                           this.close(player);
                        } else {
                           transactionMenu.open(true);
                        }
                     } catch (RuntimeException exception) {
                        if (flag) {
                           SendMessage.errorMessage("Sell completed but post-processing failed: " + exception.getMessage());
                        } else {
                           player.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                           SendMessage.errorMessage("Rolled back a failed sell: " + exception.getMessage());
                           result = Transaction.Result.TRANSACTION_CANCELLED;
                        }
                     }
                  } else {
                     transactionMenu.emptyCache();
                     result = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.plugin.getSoundEffects().insufficientStock(player);
               transactionMenu.emptyCache();
               result = Transaction.Result.REACHED_SELL_LIMIT;
               if (transactionMenu.getSellLimit() == -1 || transactionMenu.getSellLimit() == -2) {
                  transactionMenu.setSelectedItem(true);
               }
            }
         } else {
            SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get());
            transactionMenu.emptyCache();
            result = Transaction.Result.NO_ITEMS_FOUND;
         }
      }

      }

      Bukkit.getPluginManager()
         .callEvent(
            new PostTransactionEvent(
               Collections.singletonMap(transactionMenu.getShopItem(), value), (Map<EcoType, Double>)map, player, value, transactionMenu.getTransactionType(), result
            )
         );
   }

   public void completeSellAll(TransactionMenu transactionMenu, Player player) {
      Transaction.Result result = Transaction.Result.SUCCESS;
      Double availableItems;
      synchronized (this.getSellLock()) {
      availableItems = transactionMenu.getAvailableItems(Integer.MAX_VALUE);
      if (availableItems != null) {
         if (availableItems >= 0.0) {
            if (transactionMenu.getShopItem().getEcoType().getType() == EconomyType.ITEM && !this.canPay(player, transactionMenu.getShopItem().getEcoType(), availableItems)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get());
               result = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               PreTransactionEvent preTransactionEvent = new PreTransactionEvent(transactionMenu.getShopItem(), player, transactionMenu.getAmount(), availableItems, transactionMenu.getTransactionType());
               Bukkit.getPluginManager().callEvent(preTransactionEvent);
               availableItems = preTransactionEvent.getPrice();
               if (!preTransactionEvent.isCancelled()) {
                  if (availableItems >= 0.0) {
                     final int sold = transactionMenu.getAmount();
                     final List<ItemStack> removedStacks = transactionMenu.takeCachedItems();
                     boolean flag = false;
                     try {
                        this.plugin.getEcoHandler().getEcon(transactionMenu.getShopItem().getEcoType()).depositBalance(player, availableItems);
                        flag = true;
                        if (transactionMenu.getShopItem().isSellCommand() && this.plugin.commandItems) {
                           CommandHelper.executeCommandItem(transactionMenu.getShopItem(), player, CommandType.SELL, String.valueOf(availableItems), sold);
                           result = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                        }

                        transactionMenu.sendTransactionCompleteMessage(availableItems);
                        this.plugin.runTaskAsync(() -> this.sellItems(transactionMenu.getShopItem(), sold, player.getUniqueId()));
                        if (transactionMenu.getShopItem().getLimitedSellMode() != 0) {
                           transactionMenu.updateSellLimit(sold);
                           this.plugin.getLimitedSellProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold, false);
                        }

                        if (transactionMenu.getShopItem().isRefillStock()) {
                           transactionMenu.addItemStock(sold);
                           this.plugin.getLimitedStockProvider().sellItems(transactionMenu.getShopItem(), player.getUniqueId(), sold);
                        }

                        if (transactionMenu.getShopItem().isCloseMenu()) {
                           this.close(player);
                        } else {
                           transactionMenu.open(true);
                        }
                     } catch (RuntimeException exception) {
                        if (flag) {
                           SendMessage.errorMessage("Sell completed but post-processing failed: " + exception.getMessage());
                        } else {
                           player.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                           SendMessage.errorMessage("Rolled back a failed sell: " + exception.getMessage());
                           result = Transaction.Result.TRANSACTION_CANCELLED;
                        }
                     }
                  } else {
                     transactionMenu.emptyCache();
                     result = Transaction.Result.NEGATIVE_ITEM_PRICE;
                  }
               } else {
                  transactionMenu.emptyCache();
                  result = Transaction.Result.TRANSACTION_CANCELLED;
               }
            }
         } else {
            SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
            this.plugin.getSoundEffects().insufficientStock(player);
            transactionMenu.emptyCache();
            result = Transaction.Result.REACHED_SELL_LIMIT;
            if (transactionMenu.getSellLimit() == -1 || transactionMenu.getSellLimit() == -2) {
               transactionMenu.setSelectedItem(true);
            }
         }
      } else {
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_TO_BE_SOLD.get());
         transactionMenu.emptyCache();
         result = Transaction.Result.NO_ITEMS_FOUND;
         availableItems = 0.0;
      }

      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(transactionMenu.getShopItem(), player, transactionMenu.getAmount(), availableItems, transactionMenu.getTransactionType(), result));
   }

   private boolean arePricesValid(Map<EcoType, Double> map) {
      for (Double amount : map.values()) {
         if (amount == null || !(amount >= 0.0)) {
            return false;
         }
      }

      return true;
   }

   private boolean canPay(Map<EcoType, Double> map, Player player) {
      return map.entrySet()
         .stream()
         .filter(entry -> ((EcoType)entry.getKey()).getType() == EconomyType.ITEM)
         .allMatch(entry -> this.canPay(player, (EcoType)entry.getKey(), (Double)entry.getValue()));
   }

   private boolean canPay(Player player, EcoType ecoType, double amount) {
      int value = 0;
      int maxStackSize = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(ecoType)).getItem().getMaxStackSize();

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack == null || itemStack.getType().equals(Material.AIR)) {
            value += maxStackSize;
         } else if (itemStack.isSimilar(((ItemEconomy)this.plugin.getEcoHandler().getEcon(ecoType)).getItem()) && itemStack.getAmount() < maxStackSize) {
            value += maxStackSize - itemStack.getAmount();
         }
      }

      return value >= amount;
   }

   public boolean isAllowedGamemode(Player player) {
      if (this.plugin.bannedGamemodes.contains(player.getGameMode()) && !PermissionsCache.hasPermission(player, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(player, Lang.CANNOT_ENTER_SHOP_BANNED_GAMEMODE.get().replace("%gamemode%", player.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }

   private void sellItems(ShopItem shopItem, int value, UUID uniqueId) {
      if (this.plugin.dynamicPricing) {
         this.plugin.getDynamicPricingProvider().sellItem(shopItem.getItemPath(), value);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onMenuClose(InventoryCloseEvent event) {
      if (InventoryUtil.getHolder(event.getInventory()) instanceof ShopInventory) {
         Player player = (Player)event.getPlayer();
         InventoryHolder holder = InventoryUtil.getHolder(event.getInventory());
         boolean flag = backOnClose && !UserManager.getUser(player).isOpenNewGUI();
         this.plugin.runTaskLaterFor(() -> this.closeInventory(player, holder, flag), player, 1L);
      } else if (InventoryUtil.getHolder(event.getInventory()) instanceof ItemEditor) {
         Player player = (Player)event.getPlayer();
         if (InventoryUtil.getHolder(event.getInventory()) instanceof FireworkOptions) {
            this.plugin.itemEditor.saveFireworkOptions(event.getInventory());
         } else if (InventoryUtil.getHolder(event.getInventory()) instanceof DynamicPricingOptions) {
            this.plugin.itemEditor.saveDynamicPricingOptions(event.getInventory());
         } else if (InventoryUtil.getHolder(event.getInventory()) instanceof AvailableOptions) {
            this.plugin.itemEditor.saveUnsavedOptions(event.getInventory());
         }

         this.plugin.runTaskLaterFor(player::updateInventory, player, 1L);
      }
   }

   private void closeInventory(Player player, InventoryHolder holder, boolean flag) {
      if (flag && holder instanceof ShopPage) {
         ShopPage shopPage = (ShopPage)holder;
         if (!shopPage.isDisabledBackButton() || shopPage.isSubSection() && !BackButton.SUB_SECTIONS) {
            if (shopPage.isSubSection()) {
               this.plugin.getSection(shopPage.getRootSection()).openShopSection(player, shopPage.isDisabledBackButton());
            } else {
               MainMenu.open(player);
            }

            this.plugin.getSoundEffects().useBackButton(player);
         }
      } else if (flag && holder instanceof TransactionMenu) {
         TransactionMenu transactionMenu = (TransactionMenu)holder;
         if (transactionMenu.getTransactionType() != Transaction.Type.BUY_STACKS_SCREEN) {
            ShopSection section = this.plugin.getSection(transactionMenu.getShopItem().section);
            section.openShopSection(player, section.getPageForShopItem(transactionMenu.getShopItem().itemLoc), transactionMenu.isDisabledBackButton());
            this.plugin.getSoundEffects().useBackButton(player);
         } else {
            new TransactionMenu(
                  player,
                  transactionMenu.getShopItem(),
                  transactionMenu.getRootSection(),
                  transactionMenu.isDisabledBackButton(),
                  Transaction.Mode.getFromType(transactionMenu.getTransactionType()),
                  Transaction.Type.BUY_SCREEN,
                  transactionMenu.getAmount()
               )
               .open(false);
         }
      } else if (holder instanceof SellGUI) {
         ((SellGUI)holder).sellItems(player);
      }

      player.updateInventory();
   }

   private Object getSellLock() {
      return this.plugin.sellLimits && this.plugin.getLimitedSellProvider() != null ? this.plugin.getLimitedSellProvider().getSellLock() : this;
   }

   private void close(Player player) {
      UserManager.getUser(player).setOpenNewGUI(true);
      player.closeInventory();
   }

   private void handleShiftSellGUI(InventoryClickEvent inventoryClickEvent, Inventory inventory, InventoryHolder holder, int value) {
      ItemStack itemStack = inventoryClickEvent.getCurrentItem();
      if (itemStack != null) {
         ItemStack itemStack2 = itemStack.clone();
         ArrayList<Integer> values = new ArrayList<>();
         ItemStack[] contents = holder.getInventory().getContents();

         for (int index = 0; index < contents.length; index++) {
            ItemStack itemStack3 = contents[index];
            if (itemStack3 != null && itemStack3.isSimilar(itemStack2)) {
               int size = Math.min(itemStack3.getMaxStackSize(), itemStack2.getMaxStackSize());
               int amount = size - itemStack3.getAmount();
               if (amount > 0) {
                  int value3 = Math.min(amount, itemStack2.getAmount());
                  itemStack2.setAmount(itemStack2.getAmount() - value3);
                  values.add(index);
                  if (itemStack2.getAmount() <= 0) {
                     break;
                  }
               }
            }
         }

         if (itemStack2.getAmount() > 0) {
            for (int index = 0; index < contents.length; index++) {
               if (contents[index] == null) {
                  int value3 = Math.min(itemStack2.getAmount(), itemStack2.getMaxStackSize());
                  itemStack2.setAmount(itemStack2.getAmount() - value3);
                  values.add(index);
                  if (itemStack2.getAmount() <= 0) {
                     break;
                  }
               }
            }
         }

         for (int value2 : values) {
            if (this.plugin.navBar.isEnableSellGUINav() && value2 >= inventory.getSize() - 9 && value2 <= inventory.getSize() - 1) {
               inventoryClickEvent.setCancelled(true);
               return;
            }

            if (SellGUI.isFillItem(value2)) {
               inventoryClickEvent.setCancelled(true);
               return;
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onDrag(InventoryDragEvent event) {
      InventoryHolder holder = InventoryUtil.getHolder(event.getInventory());
      if (holder instanceof ShopInventory || holder instanceof ItemEditor) {
         int size = event.getInventory().getSize();

         for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < size) {
               if (holder instanceof SellGUI
                  && !SellGUI.isFillItem(rawSlot)
                  && !(this.plugin.navBar.isEnableSellGUINav() && rawSlot >= size - 9 && rawSlot <= size - 1)) {
                  continue;
               }

               event.setCancelled(true);
               event.setResult(Result.DENY);
               return;
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onCreative(InventoryCreativeEvent event) {
      InventoryHolder holder = InventoryUtil.getHolder(event.getInventory());
      if (holder instanceof ShopInventory || holder instanceof ItemEditor) {
         event.setCancelled(true);
         event.setResult(Result.DENY);
      }
   }

   public boolean purchaseItem(Player player, ShopItem shopItem) {
      int stackSize = shopItem.getStackSize();
      double amount = 0.0;
      if (!PlayerItems.canBuy(player, shopItem)) {
         SendMessage.chatToPlayer(player, Lang.MORE_SPACE_NEEDED.get());
         this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.NO_INVENTORY_SPACE);
         return false;
      } else {
         stackSize = PlayerItems.getFinalAmount(shopItem, player, stackSize);
         if (shopItem.isMinBuy(stackSize)) {
            SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get().replace("%min-buy%", String.valueOf(shopItem.getMinBuy())));
            this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            if (shopItem.getLimitedStockMode() != 0) {
               int itemStock = this.plugin.getLimitedStockProvider().getItemStock(shopItem, player.getUniqueId(), true);
               stackSize = Math.min(stackSize, itemStock);
               if (itemStock <= 0) {
                  SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(player);
                  this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.NO_ITEM_STOCK_LEFT);
                  return false;
               }
            }

            PreTransactionEvent preTransactionEvent = new PreTransactionEvent(shopItem, player, stackSize, shopItem.getBuyPrice(player, stackSize), Transaction.Type.QUICK_BUY);
            Bukkit.getPluginManager().callEvent(preTransactionEvent);
            amount = preTransactionEvent.getPrice();
            if (preTransactionEvent.isCancelled()) {
               this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.TRANSACTION_CANCELLED);
               return false;
            } else if (!(amount >= 0.0)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
               this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.NEGATIVE_ITEM_PRICE);
               return false;
            } else if (this.plugin.getEcoHandler().getEcon(shopItem).getBalance(player) < amount) {
               SendMessage.chatToPlayer(
                  player, Lang.INSUFFICIENT_MONEY.get().replace("%currency%", this.plugin.getEcoHandler().getEcon(shopItem.getEcoType()).getFriendly())
               );
               this.plugin.getSoundEffects().insufficientFunds(player);
               this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
               return false;
            } else {
               if (!this.plugin.getEcoHandler().getEcon(shopItem).withdrawBalance(player, amount)) {
                  SendMessage.chatToPlayer(
                     player, Lang.INSUFFICIENT_MONEY.get().replace("%currency%", this.plugin.getEcoHandler().getEcon(shopItem.getEcoType()).getFriendly())
                  );
                  this.plugin.getSoundEffects().insufficientFunds(player);
                  this.callPost(shopItem, player, stackSize, amount, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                  return false;
               }

               if (shopItem.isBuyCommand() && this.plugin.commandItems) {
                  CommandHelper.executeCommandItem(shopItem, player, CommandType.BUY, String.valueOf(amount), stackSize);
               }

               if (!shopItem.isBuyCommand() || shopItem.isGiveItem()) {
                  PlayerItems.giveItems(player, shopItem, stackSize);
               }

               boolean flag = false;
               if (shopItem.getLimitedStockMode() != 0) {
                  flag = true;
                  if (true) {
                     this.plugin.getLimitedStockProvider().buyItems(shopItem, player.getUniqueId(), stackSize);
                  }
               }

               if (this.plugin.dynamicPricing && shopItem.isDynamicPricing()) {
                  flag = true;
                  if (true) {
                     this.plugin.getDynamicPricingProvider().buyItem(shopItem.getItemPath(), stackSize);
                  }
               }

               SendMessage.sendTransactionMessage(
                  player, stackSize, amount, shopItem, Transaction.Mode.BUY, Transaction.Type.QUICK_BUY, shopItem.isBuyCommand() && !shopItem.isGiveItem()
               );
               if (shopItem.isCloseMenu()) {
                  this.close(player);
               }

               this.callPost(
                  shopItem,
                  player,
                  stackSize,
                  amount,
                  Transaction.Type.QUICK_BUY,
                  shopItem.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
               );
               return flag;
            }
         }
      }
   }

   public boolean purchaseItemA(Player player, ShopItem shopItem) {
      int stackSize = shopItem.getStackSize();
      HashMap itemIds = new HashMap();
      if (!PlayerItems.canBuy(player, shopItem)) {
         SendMessage.chatToPlayer(player, Lang.MORE_SPACE_NEEDED.get());
         this.callPost(shopItem, player, stackSize, itemIds, Transaction.Type.QUICK_BUY, Transaction.Result.NO_INVENTORY_SPACE);
         return false;
      } else {
         stackSize = PlayerItems.getFinalAmount(shopItem, player, stackSize);
         if (shopItem.isMinBuy(stackSize)) {
            SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get().replace("%min-buy%", String.valueOf(shopItem.getMinBuy())));
            this.callPost(shopItem, player, stackSize, itemIds, Transaction.Type.QUICK_BUY, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            if (shopItem.getLimitedStockMode() != 0) {
               int itemStock = this.plugin.getLimitedStockProvider().getItemStock(shopItem, player.getUniqueId(), true);
               stackSize = Math.min(stackSize, itemStock);
               if (itemStock <= 0) {
                  SendMessage.chatToPlayer(player, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(player);
                  this.callPost(shopItem, player, stackSize, itemIds, Transaction.Type.QUICK_BUY, Transaction.Result.NO_ITEM_STOCK_LEFT);
                  return false;
               }
            }

            PreTransactionEvent preTransactionEvent = new PreTransactionEvent(
               Collections.singletonMap(shopItem, stackSize),
               shopItem.getABuyPrice().getBuyPrices(null, player, stackSize, shopItem.getBuyModifiers()),
               player,
               stackSize,
               Transaction.Type.QUICK_BUY
            );
            Bukkit.getPluginManager().callEvent(preTransactionEvent);
            Map prices = preTransactionEvent.getPrices();
            if (preTransactionEvent.isCancelled()) {
               this.callPost(shopItem, player, stackSize, prices, Transaction.Type.QUICK_BUY, Transaction.Result.TRANSACTION_CANCELLED);
               return false;
            } else if (!this.arePricesValid(prices)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_PURCHASE_ITEM.get());
               this.callPost(shopItem, player, stackSize, prices, Transaction.Type.QUICK_BUY, Transaction.Result.NEGATIVE_ITEM_PRICE);
               return false;
            } else {
               Map map = this.plugin.getEcoHandler().has(prices, player);
               if (map.size() != 0) {
                  SendMessage.chatToPlayer(
                     player,
                     Lang.INSUFFICIENT_MONEY
                        .get()
                        .replace(
                           "%currency%",
                           this.plugin
                              .formatPrices(Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0], map.keySet(), shopItem.getABuyPrice().isAll())
                        )
                  );
                  this.plugin.getSoundEffects().insufficientFunds(player);
                  this.callPost(shopItem, player, stackSize, prices, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                  return false;
               } else {
                  if (!this.plugin.getEcoHandler().withdraw(prices, player)) {
                     SendMessage.chatToPlayer(
                        player,
                        Lang.INSUFFICIENT_MONEY
                           .get()
                           .replace(
                              "%currency%",
                              this.plugin.formatPrices(
                                 Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0], prices.keySet(), shopItem.getABuyPrice().isAll()
                              )
                           )
                     );
                     this.plugin.getSoundEffects().insufficientFunds(player);
                     this.callPost(shopItem, player, stackSize, prices, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                     return false;
                  }

                  if (shopItem.isBuyCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(shopItem, player, CommandType.BUY, prices.toString(), stackSize);
                  }

                  if (!shopItem.isBuyCommand() || shopItem.isGiveItem()) {
                     PlayerItems.giveItems(player, shopItem, stackSize);
                  }

                  boolean flag = false;
                  if (shopItem.getLimitedStockMode() != 0) {
                     flag = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().buyItems(shopItem, player.getUniqueId(), stackSize);
                     }
                  }

                  if (this.plugin.dynamicPricing && shopItem.isDynamicPricing()) {
                     flag = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().buyItem(shopItem.getItemPath(), stackSize);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     player, stackSize, prices, shopItem, Transaction.Mode.BUY, Transaction.Type.QUICK_BUY, shopItem.isBuyCommand() && !shopItem.isGiveItem()
                  );
                  if (shopItem.isCloseMenu()) {
                     this.close(player);
                  }

                  this.callPost(
                     shopItem,
                     player,
                     stackSize,
                     prices,
                     Transaction.Type.QUICK_BUY,
                     shopItem.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return flag;
               }
            }
         }
      }
   }

   public boolean sellItem(Player player, ShopItem shopItem, int value) {
      double amount = 0.0;
      if (!PlayerItems.canSell(shopItem, player)) {
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get());
         this.callPost(shopItem, player, value, amount, Transaction.Type.QUICK_SELL, Transaction.Result.NO_ITEMS_FOUND);
         return false;
      } else {
         if (shopItem.getLimitedSellMode() != 0) {
            int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
            value = Math.min(value, itemLimit);
            if (value <= 0) {
               SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.callPost(shopItem, player, value, amount, Transaction.Type.QUICK_SELL, Transaction.Result.REACHED_SELL_LIMIT);
               return false;
            }
         }

         ArrayList<ItemStack> sellAble = PlayerItems.getSellAble(shopItem, player, value);
         value = sellAble.stream().mapToInt(ItemStack::getAmount).sum();
         if (shopItem.isMinSell(value)) {
            SendMessage.chatToPlayer(
               player, Lang.NOT_ENOUGH_ITEMS_TO_SELL.get().replace("%min-sell%", String.valueOf(shopItem.getMinSell())).replace("%quantity%", String.valueOf(value))
            );
            this.callPost(shopItem, player, value, amount, Transaction.Type.QUICK_SELL, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            amount = sellAble.stream().mapToDouble(value2 -> shopItem.getSellPrice(player, value2)).sum();
            if (shopItem.getEcoType().getType() == EconomyType.ITEM && !this.canPay(player, shopItem.getEcoType(), amount)) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get());
               this.callPost(shopItem, player, value, amount, Transaction.Type.QUICK_SELL, Transaction.Result.CANT_STORE_PAYMENT);
               return false;
            } else {
               PreTransactionEvent preTransactionEvent = new PreTransactionEvent(shopItem, player, value, amount, Transaction.Type.QUICK_SELL);
               Bukkit.getPluginManager().callEvent(preTransactionEvent);
               amount = preTransactionEvent.getPrice();
               if (preTransactionEvent.isCancelled()) {
                  this.callPost(shopItem, player, value, amount, Transaction.Type.QUICK_SELL, Transaction.Result.TRANSACTION_CANCELLED);
                  return false;
               } else {
                  this.plugin.getEcoHandler().getEcon(shopItem.getEcoType()).depositBalance(player, amount);
                  if (shopItem.isSellCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(shopItem, player, CommandType.SELL, String.valueOf(amount), value);
                  }

                  if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                     sellAble.forEach(entry -> player.getInventory().removeItem(new ItemStack[]{entry}));
                  }

                  boolean flag = false;
                  if (shopItem.getLimitedSellMode() != 0) {
                     flag = true;
                     if (true) {
                        this.plugin.getLimitedSellProvider().sellItems(shopItem, player.getUniqueId(), value, false);
                     }
                  }

                  if (shopItem.isRefillStock()) {
                     flag = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().sellItems(shopItem, player.getUniqueId(), value);
                     }
                  }

                  if (this.plugin.dynamicPricing && shopItem.isDynamicPricing()) {
                     flag = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().sellItem(shopItem.getItemPath(), value);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     player, value, amount, shopItem, Transaction.Mode.SELL, Transaction.Type.QUICK_SELL, shopItem.isSellCommand() && !shopItem.isRequireItem()
                  );
                  if (shopItem.isCloseMenu()) {
                     this.close(player);
                  }

                  this.callPost(
                     shopItem,
                     player,
                     value,
                     amount,
                     Transaction.Type.QUICK_SELL,
                     shopItem.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return flag;
               }
            }
         }
      }
   }

   public boolean sellItemA(Player player, ShopItem shopItem, int value) {
      HashMap<EcoType, Double> map = new HashMap<>();
      if (!PlayerItems.canSell(shopItem, player)) {
         SendMessage.chatToPlayer(player, Lang.NO_ITEM_FOUND.get());
         this.callPost(shopItem, player, value, map, Transaction.Type.QUICK_SELL, Transaction.Result.NO_ITEMS_FOUND);
         return false;
      } else {
         if (shopItem.getLimitedSellMode() != 0) {
            int itemLimit = this.plugin.getLimitedSellProvider().getItemLimit(shopItem, player.getUniqueId(), true);
            value = Math.min(value, itemLimit);
            if (value <= 0) {
               SendMessage.chatToPlayer(player, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.callPost(shopItem, player, value, map, Transaction.Type.QUICK_SELL, Transaction.Result.REACHED_SELL_LIMIT);
               return false;
            }
         }

         ArrayList<ItemStack> sellAble = PlayerItems.getSellAble(shopItem, player, value);
         value = sellAble.stream().mapToInt(ItemStack::getAmount).sum();
         if (shopItem.isMinSell(value)) {
            SendMessage.chatToPlayer(
               player, Lang.NOT_ENOUGH_ITEMS_TO_SELL.get().replace("%min-sell%", String.valueOf(shopItem.getMinSell())).replace("%quantity%", String.valueOf(value))
            );
            this.callPost(shopItem, player, value, map, Transaction.Type.QUICK_SELL, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            for (ItemStack itemStack : sellAble) {
               for (Entry entry : shopItem.getASellPrice().getSellPrices(null, player, itemStack, shopItem.getSellModifiers()).entrySet()) {
                  map.put((EcoType)entry.getKey(), map.getOrDefault(entry.getKey(), 0.0) + (Double)entry.getValue());
               }
            }

            if (map.entrySet()
               .stream()
               .anyMatch(
                  entry -> ((EcoType)entry.getKey()).getType() == EconomyType.ITEM && !this.canPay(player, (EcoType)entry.getKey(), (Double)entry.getValue())
               )) {
               SendMessage.chatToPlayer(player, Lang.CANNOT_STORE_PAYMENT.get());
               this.callPost(shopItem, player, value, map, Transaction.Type.QUICK_SELL, Transaction.Result.CANT_STORE_PAYMENT);
               return false;
            } else {
               PreTransactionEvent preTransactionEvent = new PreTransactionEvent(Collections.singletonMap(shopItem, value), map, player, value, Transaction.Type.QUICK_SELL);
               Bukkit.getPluginManager().callEvent(preTransactionEvent);
               Map prices = preTransactionEvent.getPrices();
               if (preTransactionEvent.isCancelled()) {
                  this.callPost(shopItem, player, value, prices, Transaction.Type.QUICK_SELL, Transaction.Result.TRANSACTION_CANCELLED);
                  return false;
               } else {
                  this.plugin.getEcoHandler().deposit(prices, player);
                  if (shopItem.isSellCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(shopItem, player, CommandType.SELL, prices.toString(), value);
                  }

                  if (!shopItem.isSellCommand() || shopItem.isRequireItem()) {
                     sellAble.forEach(entry -> player.getInventory().removeItem(new ItemStack[]{entry}));
                  }

                  boolean flag = false;
                  if (shopItem.getLimitedSellMode() != 0) {
                     flag = true;
                     if (true) {
                        this.plugin.getLimitedSellProvider().sellItems(shopItem, player.getUniqueId(), value, false);
                     }
                  }

                  if (shopItem.isRefillStock()) {
                     flag = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().sellItems(shopItem, player.getUniqueId(), value);
                     }
                  }

                  if (this.plugin.dynamicPricing && shopItem.isDynamicPricing()) {
                     flag = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().sellItem(shopItem.getItemPath(), value);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     player, value, prices, shopItem, Transaction.Mode.SELL, Transaction.Type.QUICK_SELL, shopItem.isSellCommand() && !shopItem.isRequireItem()
                  );
                  if (shopItem.isCloseMenu()) {
                     this.close(player);
                  }

                  this.callPost(
                     shopItem,
                     player,
                     value,
                     prices,
                     Transaction.Type.QUICK_SELL,
                     shopItem.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return flag;
               }
            }
         }
      }
   }

   private void callPost(ShopItem shopItem, Player player, int value, double amount, Transaction.Type type, Transaction.Result result) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(shopItem, player, value, amount, type, result));
   }

   private void callPost(ShopItem shopItem, Player player, int value, Map<EcoType, Double> map, Transaction.Type type, Transaction.Result result) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(Collections.singletonMap(shopItem, value), map, player, value, type, result));
   }

   private String getCommand(String command, ShopItem shopItem, int value, double amount) {
      return command.replace("%quantity%", String.valueOf(value))
         .replace("%price%", this.plugin.formatPrice(shopItem.getEcoType(), amount))
         .replace("%buyPrice%", this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getBuyPrice(value)))
         .replace("%sellPrice%", this.plugin.formatPrice(shopItem.getEcoType(), shopItem.getSellPrice()));
   }
}
