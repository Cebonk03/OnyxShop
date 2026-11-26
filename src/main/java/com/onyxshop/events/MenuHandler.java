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

   public MenuHandler(OnyxShop var1) {
      this.plugin = var1;
      int var2 = Math.max(0, ConfigManager.getConfig().getInt("click-cooldown", 125));
      this.cooldown = (ConcurrentMap<UUID, Boolean>)(ConcurrentMap<?, ?>) CacheBuilder.newBuilder().expireAfterWrite(Duration.of(var2, ChronoUnit.MILLIS)).build().asMap();
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onMenuClick(InventoryClickEvent var1) {
      int var2 = var1.getRawSlot();
      Inventory var3 = var1.getInventory();
      InventoryHolder var4 = InventoryUtil.getHolder(var3);
      if (var4 instanceof ShopInventory || var4 instanceof ItemEditor) {
         if (var2 >= var3.getSize()) {
            if (var1.getClick() != ClickType.SHIFT_LEFT && var1.getClick() != ClickType.SHIFT_RIGHT) {
               if (var1.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)) {
                  var1.setCancelled(true);
               }
            } else if (var4 instanceof SellGUI) {
               this.handleShiftSellGUI(var1, var3, var4, var2);
            } else {
               var1.setCancelled(true);
            }
         } else {
            var1.setCancelled(true);
            if (var1.getClick() != ClickType.DOUBLE_CLICK) {
               Player var5 = (Player)var1.getWhoClicked();
               if (this.cooldown.putIfAbsent(var5.getUniqueId(), Boolean.TRUE) == null) {
                  if (var1.getCurrentItem() != null && var1.getCurrentItem().getType() != Material.AIR || var4 instanceof SellGUI) {
                     if (var4 instanceof ShopPage) {
                        ShopPage var12 = (ShopPage)var4;
                        if (var2 >= var3.getSize() - 9 && var2 <= var3.getSize() - 1 && this.plugin.navBar.isEnabled(var12.getSection())) {
                           this.plugin.navBar.execute(var5, var12, var2, var12.getPage(), var12.isDisabledBackButton());
                        } else {
                           DisplayItem var15 = this.plugin.getDisplayItem(var12.getSection(), var12.getItem(var2));
                           if (var15 == null) {
                              return;
                           }

                           ClickAction var17 = this.plugin.getSection(var12.getSection()).getClickAction(var1.getClick());
                           if (var17 == null) {
                              return;
                           }

                           if (var17 == ClickAction.GUI_EDITOR
                              && var15 instanceof ShopItem
                              && PermissionsCache.hasPermission(var5, "OnyxShop.editshop.editor")) {
                              new Main(var5, (ShopItem)var15);
                           } else if (!var15.hasItemError()) {
                              if (!var15.canPurchase(var5, "shop", false) || !this.isAllowedGamemode(var5)) {
                                 return;
                              }

                              if (var15.meetsRequirements(var5, false)) {
                                 if (var15.isClickCommand() && this.plugin.commandItems) {
                                    CommandHelper.executeCommandItem(var15, var5, CommandType.CLICK);
                                 }

                                 if (var15 instanceof ActionItem) {
                                    this.plugin.navBar.execute(var5, var12, ((ActionItem)var15).getAction(), var12.getPage(), var12.isDisabledBackButton());
                                    return;
                                 }

                                 ShopItem var9 = (ShopItem)var15;
                                 if (var9.isLinked()) {
                                    if (PermissionsCache.hasPermission(var5, "OnyxShop.shop." + var9.getSubSection().toLowerCase())) {
                                       this.plugin.getSection(var9.getSubSection()).openShopSection(var5, var12.isDisabledBackButton(), var9.section);
                                    } else {
                                       SendMessage.chatToPlayer(var5, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                                    }

                                    return;
                                 }

                                 if (var9.isDisplayItem()) {
                                    return;
                                 }

                                 if (var17 == ClickAction.BUY) {
                                    if (!var9.isBuyAble()) {
                                       return;
                                    }

                                    if (var9.getMaxBuy() == -1) {
                                       if (var9.isABuyPricing()) {
                                          if (this.purchaseItemA(var5, var9)) {
                                             var3.setItem(var1.getSlot(), this.plugin.getSection(var9.section).updateItem(var5, var9));
                                          }
                                       } else if (this.purchaseItem(var5, var9)) {
                                          var3.setItem(var1.getSlot(), this.plugin.getSection(var9.section).updateItem(var5, var9));
                                       }
                                    } else {
                                       new TransactionMenu(
                                             var5,
                                             var9,
                                             var12.getRootSection(),
                                             var12.isDisabledBackButton(),
                                             Transaction.Mode.BUY,
                                             Transaction.Type.BUY_SCREEN,
                                             1
                                          )
                                          .open(false);
                                    }
                                 } else if (var17 == ClickAction.SELL_ALL) {
                                    if (!var9.isSellAble() || !this.plugin.MMB) {
                                       return;
                                    }

                                    if (var9.isASellPricing() ? this.sellItemA(var5, var9, Integer.MAX_VALUE) : this.sellItem(var5, var9, Integer.MAX_VALUE)) {
                                       var3.setItem(var1.getSlot(), this.plugin.getSection(var9.section).updateItem(var5, var9));
                                    }
                                 } else if (var17 == ClickAction.SELL) {
                                    if (!var9.isSellAble()) {
                                       return;
                                    }

                                    if (var9.getMaxSell() == -1) {
                                       if (var9.isASellPricing()) {
                                          if (this.sellItemA(var5, var9, var9.getStackSize())) {
                                             var3.setItem(var1.getSlot(), this.plugin.getSection(var9.section).updateItem(var5, var9));
                                          }
                                       } else if (this.sellItem(var5, var9, var9.getStackSize())) {
                                          var3.setItem(var1.getSlot(), this.plugin.getSection(var9.section).updateItem(var5, var9));
                                       }
                                    } else {
                                       new TransactionMenu(
                                             var5,
                                             var9,
                                             var12.getRootSection(),
                                             var12.isDisabledBackButton(),
                                             Transaction.Mode.SELL,
                                             Transaction.Type.SELL_SCREEN,
                                             1
                                          )
                                          .open(false);
                                    }
                                 }
                              }
                           } else {
                              SendMessage.chatToPlayer(var5, Lang.ITEM_ERROR.get());
                           }
                        }
                     } else if (var4 instanceof TransactionMenu) {
                        TransactionMenu var11 = (TransactionMenu)var4;
                        if (var2 >= var3.getSize() - 9 && var2 <= var3.getSize() - 1) {
                           switch (var11.getTransactionType()) {
                              case SHOPSTAND_BUY_SCREEN:
                              case SHOPSTAND_SELL_SCREEN:
                                 if (this.plugin.navBar.isEnableShopStandsNav()) {
                                    this.plugin
                                       .navBar
                                       .execute(var11, var5, var11.getShopItem(), var2 - (var3.getSize() - 9), var11.getTransactionMode(), var11.getAmount());
                                 }
                                 break;
                              default:
                                 if (this.plugin.navBar.isEnableTransactionNav()) {
                                    this.plugin
                                       .navBar
                                       .execute(
                                          var11,
                                          var5,
                                          var11.getShopItem(),
                                          var2 - (var3.getSize() - 9),
                                          var11.getRootSection(),
                                          var11.isDisabledBackButton(),
                                          var11.getTransactionMode(),
                                          var11.getTransactionType(),
                                          var11.getAmount()
                                       );
                                 }
                           }
                        }

                        TransactionItem var14 = TransactionMenus.getItemFromSlot(var11.getTransactionType(), var2);
                        if (var14 != null) {
                           switch (var14.getAction().type) {
                              case NONE:
                                 break;
                              case BACK:
                                 if (var11.getTransactionType() != Transaction.Type.BUY_STACKS_SCREEN) {
                                    ShopSection var16 = this.plugin.getSection(var11.getShopItem().section);
                                    var16.openShopSection(
                                       var5, var16.getPageForShopItem(var11.getShopItem().itemLoc), var11.isDisabledBackButton(), var11.getRootSection()
                                    );
                                    this.plugin.getSoundEffects().useBackButton(var5);
                                 } else {
                                    new TransactionMenu(
                                          var5,
                                          var11.getShopItem(),
                                          var11.getRootSection(),
                                          var11.isDisabledBackButton(),
                                          var11.getTransactionMode(),
                                          Transaction.Type.BUY_SCREEN,
                                          var11.getAmount()
                                       )
                                       .open(false);
                                 }
                                 break;
                              case COMMAND:
                                 List<String> var8 = var14.getClickCommands();
                                 if (!var8.isEmpty()) {
                                    var8.forEach(var1x -> CommandHelper.executeCommand(null, var5, var1x));
                                 }
                                 break;
                              case INSTA_BUY:
                              case INSTA_SELL:
                                 var11.setAmount(var14.getItem().getAmount());
                              case CONFIRM_TRANSACTION:
                                 if (var1.getClick().isLeftClick()) {
                                    if (var11.isAPricing()) {
                                       this.completeTransactionA(var11, var5);
                                    } else {
                                       this.completeTransaction(var11, var5);
                                    }
                                 } else if (var1.getClick().isRightClick() && var11.isAPricing() && !var11.isAllPrices()) {
                                    var11.cycleEcoType();
                                 }
                                 break;
                              case OPEN_BUY_STACKS:
                                 if (!var11.getShopItem().isMaxBuy(var11.getMaxStackSize()) && var11.getShopItem().isAllowQuantity()) {
                                    new TransactionMenu(
                                          var5,
                                          var11.getShopItem(),
                                          var11.getRootSection(),
                                          var11.isDisabledBackButton(),
                                          Transaction.Mode.BUY,
                                          Transaction.Type.BUY_STACKS_SCREEN,
                                          var11.getAmount()
                                       )
                                       .open(false);
                                 }
                                 break;
                              case SELL_ALL:
                                 this.completeSellAll(var11, var5);
                                 break;
                              default:
                                 if (var11.getShopItem().isAllowQuantity()) {
                                    var11.updateAmount(var14.getAction());
                                    if (var1.getCurrentItem() != null && var14.getItem().getType() == var1.getCurrentItem().getType()) {
                                       this.plugin.getSoundEffects().changeItemAmount(var5);
                                    }
                                 }
                           }
                        }
                     } else if (var4 instanceof MainMenu) {
                        if (this.plugin.navBar.isEnableMainNav() && var2 >= var3.getSize() - 9 && var2 <= var3.getSize() - 1) {
                           this.plugin.navBar.execute(var4, var5, var2 - (var3.getSize() - 9));
                        } else {
                           String var10 = this.plugin.getMainMenuSectionForSlot(var2);
                           if (var10 != null && this.plugin.getSections().containsKey(var10)) {
                              if (PermissionsCache.hasPermission(var5, "OnyxShop.shop." + var10.toLowerCase())) {
                                 ShopSection var13 = this.plugin.getSection(var10);
                                 if (!var13.isCommandItem()) {
                                    var13.openShopSection(var5, false);
                                 } else {
                                    var13.getClickCommands().forEach(var1x -> CommandHelper.executeCommand(null, var5, var1x));
                                    if (var13.isCloseMenu()) {
                                       this.close(var5);
                                    }
                                 }

                                 this.plugin.getSoundEffects().openShop(var5);
                              } else if (!this.plugin.getSectionItem(var10).isHideIfNoPerms()) {
                                 SendMessage.chatToPlayer(var5, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                              }
                           }
                        }
                     } else if (var4 instanceof ShopSearch) {
                        ShopSearch var6 = (ShopSearch)var4;
                        if (this.plugin.navBar.isEnableSearchNav() && var2 >= var3.getSize() - 9 && var2 <= var3.getSize() - 1) {
                           this.plugin.navBar.execute(var6, var5, var2 - (var3.getSize() - 9), var6.getPage());
                        } else {
                           ShopItem var7 = var6.getShopItemForSlot(var2);
                           if (var1.getClick().isShiftClick() && PermissionsCache.hasPermission(var5, "OnyxShop.editshop.editor")) {
                              new Main(var5, var7);
                           } else if (!var7.hasItemError()) {
                              if (!var7.canPurchase(var5, "shop", false) || !this.isAllowedGamemode(var5)) {
                                 return;
                              }

                              if (PermissionsCache.hasPermission(var5, "OnyxShop.shop." + var7.section)) {
                                 if (var1.getClick().isLeftClick()) {
                                    if (var7.isBuyAble()) {
                                       new TransactionMenu(var5, var7, null, true, Transaction.Mode.BUY, Transaction.Type.BUY_SCREEN, 1).open(false);
                                    }
                                 } else if (var1.getClick().isRightClick() && var7.isSellAble()) {
                                    new TransactionMenu(var5, var7, null, true, Transaction.Mode.SELL, Transaction.Type.SELL_SCREEN, 1).open(false);
                                 }
                              } else {
                                 SendMessage.chatToPlayer(var5, Lang.NO_PERMISSIONS_TO_OPEN_SHOP.get());
                              }
                           } else {
                              SendMessage.chatToPlayer(var5, Lang.ITEM_ERROR.get());
                           }
                        }
                     } else if (var4 instanceof SellGUI) {
                        if (this.plugin.navBar.isEnableSellGUINav() && var2 >= var3.getSize() - 9 && var2 <= var3.getSize() - 1) {
                           this.plugin.navBar.execute(var4, var5, var2 - (var3.getSize() - 9));
                        } else if (!SellGUI.isFillItem(var2)) {
                           var1.setCancelled(false);
                        }
                     } else if (var4 instanceof ItemEditor) {
                        this.handleItemEditor(var1, var5, var3, var4, var2);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleItemEditor(InventoryClickEvent var1, Player var2, Inventory var3, InventoryHolder var4, int var5) {
      if (var4 instanceof Main) {
         Main var6 = (Main)var4;
         switch (var5) {
            case 29:
               this.plugin.itemEditor.openItemEditorCreateItem(var2, var6.getItem(), null, null, null);
               break;
            case 31:
               new AvailableOptions(var2, var6.getItem());
               break;
            case 33:
               new DeleteItemConfirmation(var2, var6.getItem());
               break;
            case 53:
               this.plugin
                  .getSection(var6.getItem().section)
                  .openShopSection(var2, this.plugin.getSection(var6.getItem().section).getPageForShopItem(var6.getItem().itemLoc), false);
         }
      } else if (var4 instanceof IndexChooser) {
         IndexChooser var9 = (IndexChooser)var4;
         Integer var7 = this.plugin.versionHandler.getOnyxNbtInt(var1.getCurrentItem(), "Index");
         if (var7 != null) {
            this.plugin.itemEditor.openAvailableResultsForOption(var2, var9.getOption(), var9.getAction(), var9.getItem(), var7);
         } else if (var5 == var3.getSize() - 1) {
            new AvailableActions(var2, var9.getItem(), var9.getOption());
         }
      } else if (var4 instanceof FireworkOptions) {
         FireworkOptions var10 = (FireworkOptions)var4;
         com.onyxshop.methodes.ItemEditor.Option var18 = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(var1.getCurrentItem())
         );
         if (var18 != null) {
            if (var18 != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_SHAPE
               && var18 != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_FLICKER
               && var18 != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_TRAIL
               && var18 != com.onyxshop.methodes.ItemEditor.Option.FIREWORK_DURATION) {
               new AvailableActions(var2, var10.getItem(), var18);
            } else {
               var3.setItem(var5, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(var1.getCurrentItem(), "value"), var18));
            }
         } else if (var5 == 17) {
            new AvailableOptions(var2, var10.getItem());
         }
      } else if (var4 instanceof DynamicPricingOptions) {
         DynamicPricingOptions var11 = (DynamicPricingOptions)var4;
         com.onyxshop.methodes.ItemEditor.Option var19 = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(var1.getCurrentItem())
         );
         if (var19 != null) {
            if (var19 == com.onyxshop.methodes.ItemEditor.Option.DYNAMIC_PRICING_ENABLE) {
               var3.setItem(var5, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(var1.getCurrentItem(), "value"), var19));
            } else {
               new AvailableActions(var2, var11.getItem(), var19);
            }
         } else if (var5 == 17) {
            new AvailableOptions(var2, var11.getItem());
         }
      } else if (var4 instanceof LimitedStockOptions) {
         LimitedStockOptions var12 = (LimitedStockOptions)var4;
         com.onyxshop.methodes.ItemEditor.Option var20 = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(var1.getCurrentItem())
         );
         if (var20 != null) {
            new AvailableActions(var2, var12.getItem(), var20);
         } else if (var5 == 17) {
            new AvailableOptions(var2, var12.getItem());
         }
      } else if (var4 instanceof DeleteItemConfirmation) {
         DeleteItemConfirmation var13 = (DeleteItemConfirmation)var4;
         switch (var5) {
            case 3:
               ConfigManager.getShop(var13.getItem().section).set("pages." + var13.getItem().itemLoc, null);
               ConfigManager.saveShop(var13.getItem().section);
               new ConclusionScreen(var2, var13.getItem(), null, com.onyxshop.methodes.ItemEditor.Action.REMOVE_ITEM, null);
            case 5:
            default:
               break;
            case 17:
               new Main(var2, var13.getItem());
         }
      } else if (var4 instanceof ConclusionScreen) {
         ConclusionScreen var14 = (ConclusionScreen)var4;
         if (var5 == 17) {
            switch (var14.getAction()) {
               case REMOVE_ITEM:
                  this.plugin.getSection(var14.getItem().section).openShopSection(var2, false);
                  break;
               case REMOVE:
               case EDIT:
               case ADD:
                  new Main(var2, this.plugin.getShopItem(var14.getItem().section, var14.getItem().itemLoc));
                  break;
               case ADD_ITEM:
                  new Main(var2, this.plugin.getShopItem(var14.getNewItem()));
            }
         }
      } else if (var4 instanceof AvailableOptions) {
         AvailableOptions var15 = (AvailableOptions)var4;
         com.onyxshop.methodes.ItemEditor.Option var21 = com.onyxshop.methodes.ItemEditor.Option.getFromKey(
            this.plugin.versionHandler.getEditOption(var1.getCurrentItem())
         );
         if (var21 != null) {
            switch (var21) {
               case ENCHANTMENT_GLINT:
                  var3.setItem(var5, this.plugin.itemEditor.toggleOption(this.plugin.versionHandler.getOnyxNbtString(var1.getCurrentItem(), "value"), var21));
                  break;
               case DYNAMIC_PRICING:
                  new DynamicPricingOptions(var2, var15.getItem());
                  break;
               case FIREWORK:
                  new FireworkOptions(var2, var15.getItem());
                  break;
               case GLOBAL_STOCK:
               case PLAYER_STOCK:
                  new LimitedStockOptions(
                     var2,
                     var15.getItem(),
                     var21 == com.onyxshop.methodes.ItemEditor.Option.GLOBAL_STOCK
                        ? LimitedStockOptions.Type.GLOBAL
                        : LimitedStockOptions.Type.PLAYER
                  );
                  break;
               default:
                  new AvailableActions(var2, var15.getItem(), var21);
            }
         } else if (var5 == 53) {
            new Main(var2, var15.getItem());
         }
      } else if (var4 instanceof AvailableActions) {
         AvailableActions var16 = (AvailableActions)var4;
         String var22 = this.plugin.versionHandler.getAction(var1.getCurrentItem());
         if (var22 != null) {
            this.plugin
               .itemEditor
               .openAvailableResultsForOption(
                  var2, var16.getOption(), com.onyxshop.methodes.ItemEditor.Action.valueOf(var22.toUpperCase(Locale.ENGLISH)), var16.getItem(), -1
               );
         } else if (var5 == 17) {
            switch (var16.getOption()) {
               case FIREWORK_SHAPE:
               case FIREWORK_FLICKER:
               case FIREWORK_TRAIL:
               case FIREWORK_DURATION:
               case FIREWORK_COLORS:
               case FIREWORK_FADE_COLORS:
                  new FireworkOptions(var2, var16.getItem());
                  break;
               case DYNAMIC_PRICING_ENABLE:
               case DYNAMIC_PRICING_MAX_STOCK:
                  new DynamicPricingOptions(var2, var16.getItem());
                  break;
               case GLOBAL_STOCK_LIMIT:
               case GLOBAL_STOCK_TIMER:
               case GLOBAL_STOCK_FREQUENCY:
                  new LimitedStockOptions(var2, var16.getItem(), LimitedStockOptions.Type.GLOBAL);
                  break;
               case PLAYER_STOCK_LIMIT:
               case PLAYER_STOCK_TIMER:
               case PLAYER_STOCK_FREQUENCY:
                  new LimitedStockOptions(var2, var16.getItem(), LimitedStockOptions.Type.PLAYER);
                  break;
               default:
                  new AvailableOptions(var2, var16.getItem());
            }
         }
      } else if (var4 instanceof ChooseValue) {
         ChooseValue var17 = (ChooseValue)var4;
         if (var5 == var3.getSize() - 1) {
            if (var17.getAction() != com.onyxshop.methodes.ItemEditor.Action.ADD) {
               new AvailableActions(var2, var17.getItem(), var17.getOption());
            } else {
               new Main(var2, var17.getItem());
            }
         } else if (var5 == var3.getSize() - 6) {
            if (var17.getPage() > 1) {
               new ChooseValue(var17, var2, var17.getPage() - 1);
            }
         } else if (var5 == var3.getSize() - 4) {
            if (var17.getPage() != var17.getAllPages()) {
               new ChooseValue(var17, var2, var17.getPage() + 1);
            }
         } else if (var5 == var3.getSize() - 5) {
            new AnvilGUI.Builder()
               .onComplete(
                  (var3x, var4x) -> {
                     this.plugin
                        .runTaskLater(
                           () -> new ChooseValue(
                              var2, var17.getItem(), var17.getOption(), var17.getAction(), var17.getIndex(), 1, var4x.toLowerCase(Locale.ROOT)
                           ),
                           1L
                        );
                     return AnvilGUI.Response.close();
                  }
               )
               .onClose(
                  var3x -> this.plugin
                     .runTaskLater(() -> new ChooseValue(var2, var17.getItem(), var17.getOption(), var17.getAction(), var17.getIndex(), var17.getPage()), 1L)
               )
               .text("Search text")
               .itemLeft(new ItemStack(Material.PAPER))
               .title(Lang.ANVILGUI_ENTER_VALUE.get().getLegacy().replace("%option%", var17.getOption().getName()))
               .plugin(this.plugin)
               .open(var2);
         } else {
            String var23 = null;
            switch (var17.getOption()) {
               case FIREWORK_COLORS:
               case FIREWORK_FADE_COLORS:
                  var23 = FireworkUtil.FireworkColor.getFromID(XMaterial.matchXMaterial(var1.getCurrentItem()).getData()).name();
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
                  var23 = var1.getCurrentItem().getType().name();
                  if (var17.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD) {
                     this.plugin.itemEditor.openItemEditorCreateItem(var2, var17.getItem(), var23, null, null);
                     return;
                  }
                  break;
               case ENCHANTMENTS:
                  new AnvilGUI.Builder()
                     .onComplete(
                        (var4x, var5x) -> {
                           try {
                              String var6x = this.plugin.enchantsProvider.getEnchants(var1.getCurrentItem()).get(0) + ":" + Integer.parseInt(var5x);
                              if (var17.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD && var17.getIndex() == -1) {
                                 List var9x = ConfigManager.getShop(var17.getItem().section)
                                    .getStringList("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey());
                                 var9x.add(var6x);
                                 ConfigManager.getShop(var17.getItem().section)
                                    .set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var9x);
                              } else {
                                 List var7x = ConfigManager.getShop(var17.getItem().section)
                                    .getStringList("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey());
                                 if (var17.getIndex() != -1) {
                                    var7x.set(var17.getIndex(), var6x);
                                 } else {
                                    var7x.clear();
                                    var7x.add(var6x);
                                 }

                                 ConfigManager.getShop(var17.getItem().section)
                                    .set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var7x);
                              }

                              ConfigManager.saveShop(var17.getItem().section);
                              this.plugin.runTaskLater(() -> new ConclusionScreen(var2, var17.getItem(), var17.getOption(), var17.getAction(), null), 1L);
                              return AnvilGUI.Response.close();
                           } catch (NumberFormatException var8x) {
                              return AnvilGUI.Response.text(Lang.NO_VALID_AMOUNT.get().getLegacy());
                           }
                        }
                     )
                     .onClose(var3x -> {
                        if (var17.getIndex() != -1) {
                           this.plugin.runTaskLater(() -> new IndexChooser(var2, var17.getItem(), var17.getOption(), var17.getAction()), 1L);
                        } else {
                           this.plugin.runTaskLater(() -> new AvailableActions(var2, var17.getItem(), var17.getOption()), 1L);
                        }
                     })
                     .text(Lang.OPTION_STRENGTH.get().getLegacy())
                     .itemLeft(new ItemStack(Material.PAPER))
                     .title(Lang.ANVILGUI_ENTER_VALUE.get().getLegacy().replace("%option%", Lang.OPTION_STRENGTH.get().getLegacy()))
                     .plugin(this.plugin)
                     .open(var2);
                  return;
               case POTIONTYPES:
                  var23 = PotionTypes.getPotionTypes(var1.getCurrentItem()).get(0).toString();
                  break;
               case SPAWNERTYPE:
                  var23 = var1.getCurrentItem().getItemMeta().getDisplayName();
                  break;
               case SUB_SECTION:
                  var23 = this.plugin.versionHandler.getNBTString(var1.getCurrentItem(), "section");
            }

            if (var17.getAction() == com.onyxshop.methodes.ItemEditor.Action.ADD && var17.getIndex() == -1) {
               if (this.plugin.itemEditor.allListKeys.contains(var17.getOption().getKey())) {
                  List var24 = ConfigManager.getShop(var17.getItem().section)
                     .getStringList("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey());
                  var24.add(var23);
                  ConfigManager.getShop(var17.getItem().section).set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var24);
               } else {
                  ConfigManager.getShop(var17.getItem().section).set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var23);
               }
            } else if (var17.getAction() == com.onyxshop.methodes.ItemEditor.Action.EDIT) {
               if (this.plugin.itemEditor.allListKeys.contains(var17.getOption().getKey())) {
                  List var8 = ConfigManager.getShop(var17.getItem().section)
                     .getStringList("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey());
                  if (var17.getIndex() != -1) {
                     var8.set(var17.getIndex(), var23);
                  } else {
                     var8.clear();
                     var8.add(var23);
                  }

                  ConfigManager.getShop(var17.getItem().section).set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var8);
               } else {
                  ConfigManager.getShop(var17.getItem().section).set("pages." + var17.getItem().itemLoc + "." + var17.getOption().getKey(), var23);
               }
            }

            ConfigManager.saveShop(var17.getItem().section);
            new ConclusionScreen(var2, var17.getItem(), var17.getOption(), var17.getAction(), null);
         }
      }
   }

   public void completeTransaction(TransactionMenu var1, Player var2) {
      Transaction.Result var3 = Transaction.Result.SUCCESS;
      double var4 = 0.0;
      int var6 = 0;
      if (var1.getTransactionMode() != Transaction.Mode.SELL) {
         if (var1.isPurchaseAble()) {
            int var7 = var1.getFreeSpace();
            if (var7 != 0) {
               var6 = var1.getAmountToSell(var7);
               if (var1.getShopItem().getLimitedStockMode() != 0 && var1.getItemStock() <= 0) {
                  SendMessage.chatToPlayer(var2, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(var2);
                  var3 = Transaction.Result.NO_ITEM_STOCK_LEFT;
                  if (var1.getItemStock() == -1 || var1.getItemStock() == -2) {
                     var1.setSelectedItem(true);
                  }
               } else {
                  PreTransactionEvent var8 = new PreTransactionEvent(var1.getShopItem(), var2, var6, var1.getPrice(var6), var1.getTransactionType());
                  Bukkit.getPluginManager().callEvent(var8);
                  var4 = var8.getPrice();
                  if (!var8.isCancelled()) {
                     if (!(var4 >= 0.0)) {
                        SendMessage.chatToPlayer(var2, Lang.CANNOT_PURCHASE_ITEM.get());
                        var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     } else if (this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).getBalance(var2) >= var4) {
                        if (!this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).withdrawBalance(var2, var4)) {
                           SendMessage.chatToPlayer(
                              var2,
                              Lang.INSUFFICIENT_MONEY
                                 .get()
                                 .replace("%currency%", this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).getFriendly())
                           );
                           this.plugin.getSoundEffects().insufficientFunds(var2);
                           var3 = Transaction.Result.INSUFFICIENT_FUNDS;
                        } else {
                           if (var1.getShopItem().getLimitedStockMode() != 0) {
                              var1.updateItemStock(var6);
                              if (var1.getItemStock() > 0) {
                                 final int stockDeltaA = var6;
                                 this.plugin.runTaskAsync(() -> this.plugin.getLimitedStockProvider().buyItems(var1.getShopItem(), var2.getUniqueId(), stockDeltaA));
                              } else {
                                 this.plugin.getLimitedStockProvider().buyItems(var1.getShopItem(), var2.getUniqueId(), var6);
                              }
                           }

                           if (var1.getShopItem().isBuyCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(var1.getShopItem(), var2, CommandType.BUY, String.valueOf(var4), var6);
                              var3 = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           if (!var1.getShopItem().isBuyCommand() || var1.getShopItem().isGiveItem()) {
                              int undelivered = PlayerItems.giveItems(var2, var1.getShopItem(), var6);
                              if (undelivered > 0 && var6 > 0) {
                                 this.plugin
                                    .getEcoHandler()
                                    .getEcon(var1.getShopItem().getEcoType())
                                    .depositBalance(var2, var4 / var6 * undelivered);
                                 if (var1.getShopItem().getLimitedStockMode() != 0) {
                                    var1.addItemStock(undelivered);
                                    this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), undelivered);
                                 }

                                 SendMessage.chatToPlayer(
                                    var2,
                                    Lang.COULD_NOT_STORE_ALL_ITEMS.get().replace("%amount%", Integer.toString(undelivered))
                                 );
                              }
                           }

                           if (this.plugin.dynamicPricing && var1.getShopItem().isDynamicPricing()) {
                              this.plugin.getDynamicPricingProvider().buyItem(var1.getItemPath(), var6);
                           }

                           var1.sendTransactionCompleteMessage(var4, var6);
                           if (var1.getShopItem().isCloseMenu()) {
                              this.close(var2);
                           } else {
                              var1.open(true);
                           }
                        }
                     } else {
                        SendMessage.chatToPlayer(
                           var2,
                           Lang.INSUFFICIENT_MONEY
                              .get()
                              .replace("%currency%", this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).getFriendly())
                        );
                        this.plugin.getSoundEffects().insufficientFunds(var2);
                        var3 = Transaction.Result.INSUFFICIENT_FUNDS;
                     }
                  } else {
                     var3 = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(var2, Lang.MORE_SPACE_NEEDED.get());
               var3 = Transaction.Result.NO_INVENTORY_SPACE;
            }
         } else {
            SendMessage.chatToPlayer(var2, Lang.CANNOT_PURCHASE_ITEM.get());
            var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
         }
      } else {
         Double var10 = var1.getAvailableItems(var1.getAmount());
         if (var10 != null) {
            if (var10 >= 0.0) {
               if (var1.getShopItem().getEcoType().getType() == EconomyType.ITEM && !this.canPay(var2, var1.getShopItem().getEcoType(), var10)) {
                  SendMessage.chatToPlayer(var2, Lang.CANNOT_STORE_PAYMENT.get());
                  var1.emptyCache();
                  var3 = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  PreTransactionEvent var11 = new PreTransactionEvent(var1.getShopItem(), var2, var6 = var1.getAmount(), var10, var1.getTransactionType());
                  Bukkit.getPluginManager().callEvent(var11);
                  var4 = var11.getPrice();
                  if (!var11.isCancelled()) {
                     if (var4 >= 0.0) {
                        final int sold = var1.getAmount();
                        final List<ItemStack> removedStacks = var1.getCachedItems();
                        var1.emptyCache();
                        boolean var15 = false;
                        try {
                           this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).depositBalance(var2, var4);
                           var15 = true;
                           if (var1.getShopItem().isSellCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(var1.getShopItem(), var2, CommandType.SELL, String.valueOf(var4), sold);
                              var3 = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           var1.sendTransactionCompleteMessage(var4);
                           this.plugin.runTaskAsync(() -> this.sellItems(var1.getShopItem(), sold, var2.getUniqueId()));
                           if (var1.getShopItem().getLimitedSellMode() != 0) {
                              var1.updateSellLimit(sold);
                              if (var1.getSellLimit() > 0) {
                                 this.plugin
                                    .runTaskAsync(
                                       () -> this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false)
                                    );
                              } else {
                                 this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false);
                              }
                           }

                           if (var1.getShopItem().isRefillStock()) {
                              var1.addItemStock(sold);
                              if (var1.getItemStock() > 0) {
                                 this.plugin
                                    .runTaskAsync(() -> this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold));
                              } else {
                                 this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold);
                              }
                           }

                           if (var1.getShopItem().isCloseMenu()) {
                              this.close(var2);
                           } else {
                              var1.open(true);
                           }
                        } catch (RuntimeException var14) {
                           if (var15) {
                              SendMessage.errorMessage("Sell completed but post-processing failed: " + var14.getMessage());
                           } else {
                              var2.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                              SendMessage.errorMessage("Rolled back a failed sell: " + var14.getMessage());
                              var3 = Transaction.Result.TRANSACTION_CANCELLED;
                           }
                        }
                     } else {
                        var1.emptyCache();
                        var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     }
                  } else {
                     var1.emptyCache();
                     var3 = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(var2, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.plugin.getSoundEffects().insufficientStock(var2);
               var1.emptyCache();
               var3 = Transaction.Result.REACHED_SELL_LIMIT;
               if (var1.getSellLimit() == -1 || var1.getSellLimit() == -2) {
                  var1.setSelectedItem(true);
               }
            }
         } else {
            SendMessage.chatToPlayer(var2, Lang.NO_ITEM_FOUND.get());
            var1.emptyCache();
            var3 = Transaction.Result.NO_ITEMS_FOUND;
         }
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var1.getShopItem(), var2, var6, var4, var1.getTransactionType(), var3));
   }

   public void completeTransactionA(TransactionMenu var1, Player var2) {
      Transaction.Result var3 = Transaction.Result.SUCCESS;
      Map<EcoType, Double> var4 = new HashMap<>();
      int var5 = 0;
      if (var1.getTransactionMode() != Transaction.Mode.SELL) {
         if (var1.isPurchaseAble()) {
            int var6 = var1.getFreeSpace();
            if (var6 != 0) {
               var5 = var1.getAmountToSell(var6);
               if (var1.getShopItem().getLimitedStockMode() != 0 && var1.getItemStock() <= 0) {
                  SendMessage.chatToPlayer(var2, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(var2);
                  var3 = Transaction.Result.NO_ITEM_STOCK_LEFT;
                  if (var1.getItemStock() == -1 || var1.getItemStock() == -2) {
                     var1.setSelectedItem(true);
                  }
               } else {
                  PreTransactionEvent var7 = new PreTransactionEvent(
                     Collections.singletonMap(var1.getShopItem(), var5), var1.getPrices(var5), var2, var5, var1.getTransactionType()
                  );
                  Bukkit.getPluginManager().callEvent(var7);
                  var4 = var7.getPrices();
                  if (!var7.isCancelled()) {
                     Map var8 = this.plugin.getEcoHandler().has((Map<EcoType, Double>)var4, var2);
                     if (!this.arePricesValid((Map<EcoType, Double>)var4)) {
                        SendMessage.chatToPlayer(var2, Lang.CANNOT_PURCHASE_ITEM.get());
                        var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
                     } else if (var8.size() == 0) {
                        if (!this.plugin.getEcoHandler().withdraw((Map<EcoType, Double>)var4, var2)) {
                           SendMessage.chatToPlayer(
                              var2,
                              Lang.INSUFFICIENT_MONEY
                                 .get()
                                 .replace(
                                    "%currency%",
                                    this.plugin.formatPrices(
                                       Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0],
                                       var4.keySet(),
                                       var1.getShopItem().getABuyPrice().isAll()
                                    )
                                 )
                           );
                           this.plugin.getSoundEffects().insufficientFunds(var2);
                           var3 = Transaction.Result.INSUFFICIENT_FUNDS;
                        } else {
                           if (var1.getShopItem().getLimitedStockMode() != 0) {
                              var1.updateItemStock(var5);
                              if (var1.getItemStock() > 0) {
                                 final int stockDeltaB = var5;
                                 this.plugin.runTaskAsync(() -> this.plugin.getLimitedStockProvider().buyItems(var1.getShopItem(), var2.getUniqueId(), stockDeltaB));
                              } else {
                                 this.plugin.getLimitedStockProvider().buyItems(var1.getShopItem(), var2.getUniqueId(), var5);
                              }
                           }

                           if (var1.getShopItem().isBuyCommand() && this.plugin.commandItems) {
                              CommandHelper.executeCommandItem(var1.getShopItem(), var2, CommandType.BUY, var4.toString(), var5);
                              var3 = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                           }

                           if (!var1.getShopItem().isBuyCommand() || var1.getShopItem().isGiveItem()) {
                              PlayerItems.giveItems(var2, var1.getShopItem(), var5);
                           }

                           if (this.plugin.dynamicPricing && var1.getShopItem().isDynamicPricing()) {
                              this.plugin.getDynamicPricingProvider().buyItem(var1.getItemPath(), var5);
                           }

                           var1.sendTransactionCompleteMessage((Map<EcoType, Double>)var4, var5);
                           if (var1.getShopItem().isCloseMenu()) {
                              this.close(var2);
                           } else {
                              var1.open(true);
                           }
                        }
                     } else {
                        SendMessage.chatToPlayer(
                           var2,
                           Lang.INSUFFICIENT_MONEY
                              .get()
                              .replace(
                                 "%currency%",
                                 this.plugin
                                    .formatPrices(
                                       Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0],
                                       var8.keySet(),
                                       var1.getShopItem().getABuyPrice().isAll()
                                    )
                              )
                        );
                        this.plugin.getSoundEffects().insufficientFunds(var2);
                        var3 = Transaction.Result.INSUFFICIENT_FUNDS;
                     }
                  } else {
                     var3 = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(var2, Lang.MORE_SPACE_NEEDED.get());
               var3 = Transaction.Result.NO_INVENTORY_SPACE;
            }
         } else {
            SendMessage.chatToPlayer(var2, Lang.CANNOT_PURCHASE_ITEM.get());
            var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
         }
      } else {
         var4 = var1.getAvailableItemsA(var1.getAmount());
         if (var4 != null) {
            if (!var4.isEmpty()) {
               if (!var4.keySet().stream().noneMatch(var0 -> var0.getType() == EconomyType.ITEM) && !this.canPay((Map<EcoType, Double>)var4, var2)) {
                  SendMessage.chatToPlayer(var2, Lang.CANNOT_STORE_PAYMENT.get());
                  var1.emptyCache();
                  var3 = Transaction.Result.CANT_STORE_PAYMENT;
               } else {
                  PreTransactionEvent var10 = new PreTransactionEvent(
                     Collections.singletonMap(var1.getShopItem(), var5), (Map<EcoType, Double>)var4, var2, var5, var1.getTransactionType()
                  );
                  Bukkit.getPluginManager().callEvent(var10);
                  var4 = var10.getPrices();
                  if (!var10.isCancelled()) {
                     final int sold = var1.getAmount();
                     final List<ItemStack> removedStacks = var1.getCachedItems();
                     var1.emptyCache();
                     boolean var15 = false;
                     try {
                        this.plugin.getEcoHandler().deposit((Map<EcoType, Double>)var4, var2);
                        var15 = true;
                        if (var1.getShopItem().isSellCommand() && this.plugin.commandItems) {
                           CommandHelper.executeCommandItem(var1.getShopItem(), var2, CommandType.SELL, var4.toString(), sold);
                           var3 = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                        }

                        var1.sendTransactionCompleteMessage((Map<EcoType, Double>)var4, sold);
                        this.plugin.runTaskAsync(() -> this.sellItems(var1.getShopItem(), sold, var2.getUniqueId()));
                        if (var1.getShopItem().getLimitedSellMode() != 0) {
                           var1.updateSellLimit(sold);
                           if (var1.getSellLimit() > 0) {
                              this.plugin
                                 .runTaskAsync(
                                    () -> this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false)
                                 );
                           } else {
                              this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false);
                           }
                        }

                        if (var1.getShopItem().isRefillStock()) {
                           var1.addItemStock(sold);
                           if (var1.getItemStock() > 0) {
                              this.plugin
                                 .runTaskAsync(() -> this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold));
                           } else {
                              this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold);
                           }
                        }

                        if (var1.getShopItem().isCloseMenu()) {
                           this.close(var2);
                        } else {
                           var1.open(true);
                        }
                     } catch (RuntimeException var14) {
                        if (var15) {
                           SendMessage.errorMessage("Sell completed but post-processing failed: " + var14.getMessage());
                        } else {
                           var2.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                           SendMessage.errorMessage("Rolled back a failed sell: " + var14.getMessage());
                           var3 = Transaction.Result.TRANSACTION_CANCELLED;
                        }
                     }
                  } else {
                     var1.emptyCache();
                     var3 = Transaction.Result.TRANSACTION_CANCELLED;
                  }
               }
            } else {
               SendMessage.chatToPlayer(var2, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.plugin.getSoundEffects().insufficientStock(var2);
               var1.emptyCache();
               var3 = Transaction.Result.REACHED_SELL_LIMIT;
               if (var1.getSellLimit() == -1 || var1.getSellLimit() == -2) {
                  var1.setSelectedItem(true);
               }
            }
         } else {
            SendMessage.chatToPlayer(var2, Lang.NO_ITEM_FOUND.get());
            var1.emptyCache();
            var3 = Transaction.Result.NO_ITEMS_FOUND;
         }
      }

      Bukkit.getPluginManager()
         .callEvent(
            new PostTransactionEvent(
               Collections.singletonMap(var1.getShopItem(), var5), (Map<EcoType, Double>)var4, var2, var5, var1.getTransactionType(), var3
            )
         );
   }

   public void completeSellAll(TransactionMenu var1, Player var2) {
      Transaction.Result var3 = Transaction.Result.SUCCESS;
      Double var4 = var1.getAvailableItems(Integer.MAX_VALUE);
      if (var4 != null) {
         if (var4 >= 0.0) {
            if (var1.getShopItem().getEcoType().getType() == EconomyType.ITEM && !this.canPay(var2, var1.getShopItem().getEcoType(), var4)) {
               SendMessage.chatToPlayer(var2, Lang.CANNOT_STORE_PAYMENT.get());
               var3 = Transaction.Result.CANT_STORE_PAYMENT;
            } else {
               PreTransactionEvent var5 = new PreTransactionEvent(var1.getShopItem(), var2, var1.getAmount(), var4, var1.getTransactionType());
               Bukkit.getPluginManager().callEvent(var5);
               var4 = var5.getPrice();
               if (!var5.isCancelled()) {
                  if (var4 >= 0.0) {
                     final int sold = var1.getAmount();
                     final List<ItemStack> removedStacks = var1.getCachedItems();
                     var1.emptyCache();
                     boolean var15 = false;
                     try {
                        this.plugin.getEcoHandler().getEcon(var1.getShopItem().getEcoType()).depositBalance(var2, var4);
                        var15 = true;
                        if (var1.getShopItem().isSellCommand() && this.plugin.commandItems) {
                           CommandHelper.executeCommandItem(var1.getShopItem(), var2, CommandType.SELL, String.valueOf(var4), sold);
                           var3 = Transaction.Result.SUCCESS_COMMANDS_EXECUTED;
                        }

                        var1.sendTransactionCompleteMessage(var4);
                        this.plugin.runTaskAsync(() -> this.sellItems(var1.getShopItem(), sold, var2.getUniqueId()));
                        if (var1.getShopItem().getLimitedSellMode() != 0) {
                           var1.updateSellLimit(sold);
                           if (var1.getSellLimit() > 0) {
                              this.plugin
                                 .runTaskAsync(
                                    () -> this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false)
                                 );
                           } else {
                              this.plugin.getLimitedSellProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold, false);
                           }
                        }

                        if (var1.getShopItem().isRefillStock()) {
                           var1.addItemStock(sold);
                           if (var1.getItemStock() > 0) {
                              this.plugin
                                 .runTaskAsync(() -> this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold));
                           } else {
                              this.plugin.getLimitedStockProvider().sellItems(var1.getShopItem(), var2.getUniqueId(), sold);
                           }
                        }

                        if (var1.getShopItem().isCloseMenu()) {
                           this.close(var2);
                        } else {
                           var1.open(true);
                        }
                     } catch (RuntimeException var14) {
                        if (var15) {
                           SendMessage.errorMessage("Sell completed but post-processing failed: " + var14.getMessage());
                        } else {
                           var2.getInventory().addItem(removedStacks.toArray(new ItemStack[0]));
                           SendMessage.errorMessage("Rolled back a failed sell: " + var14.getMessage());
                           var3 = Transaction.Result.TRANSACTION_CANCELLED;
                        }
                     }
                  } else {
                     var1.emptyCache();
                     var3 = Transaction.Result.NEGATIVE_ITEM_PRICE;
                  }
               } else {
                  var1.emptyCache();
                  var3 = Transaction.Result.TRANSACTION_CANCELLED;
               }
            }
         } else {
            SendMessage.chatToPlayer(var2, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
            this.plugin.getSoundEffects().insufficientStock(var2);
            var1.emptyCache();
            var3 = Transaction.Result.REACHED_SELL_LIMIT;
            if (var1.getSellLimit() == -1 || var1.getSellLimit() == -2) {
               var1.setSelectedItem(true);
            }
         }
      } else {
         SendMessage.chatToPlayer(var2, Lang.NO_ITEM_TO_BE_SOLD.get());
         var1.emptyCache();
         var3 = Transaction.Result.NO_ITEMS_FOUND;
         var4 = 0.0;
      }

      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var1.getShopItem(), var2, var1.getAmount(), var4, var1.getTransactionType(), var3));
   }

   private boolean arePricesValid(Map<EcoType, Double> var1) {
      for (Double var2 : var1.values()) {
         if (var2 == null || !(var2 >= 0.0)) {
            return false;
         }
      }

      return true;
   }

   private boolean canPay(Map<EcoType, Double> var1, Player var2) {
      return var1.entrySet()
         .stream()
         .filter(var0 -> ((EcoType)var0.getKey()).getType() == EconomyType.ITEM)
         .allMatch(var2x -> this.canPay(var2, (EcoType)var2x.getKey(), (Double)var2x.getValue()));
   }

   private boolean canPay(Player var1, EcoType var2, double var3) {
      int var5 = 0;
      int var6 = ((ItemEconomy)this.plugin.getEcoHandler().getEcon(var2)).getItem().getMaxStackSize();

      for (int var7 = 0; var7 < 36; var7++) {
         ItemStack var8 = var1.getInventory().getItem(var7);
         if (var8 == null || var8.getType().equals(Material.AIR)) {
            var5 += var6;
         } else if (var8.isSimilar(((ItemEconomy)this.plugin.getEcoHandler().getEcon(var2)).getItem()) && var8.getAmount() < var6) {
            var5 += var6 - var8.getAmount();
         }
      }

      return var5 >= var3;
   }

   public boolean isAllowedGamemode(Player var1) {
      if (this.plugin.bannedGamemodes.contains(var1.getGameMode()) && !PermissionsCache.hasPermission(var1, "OnyxShop.bypassgamemode")) {
         SendMessage.chatToPlayer(var1, Lang.CANNOT_ENTER_SHOP_BANNED_GAMEMODE.get().replace("%gamemode%", var1.getGameMode().name().toLowerCase()));
         return false;
      } else {
         return true;
      }
   }

   private void sellItems(ShopItem var1, int var2, UUID var3) {
      if (this.plugin.dynamicPricing) {
         this.plugin.getDynamicPricingProvider().sellItem(var1.getItemPath(), var2);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = true
   )
   public void onMenuClose(InventoryCloseEvent var1) {
      if (InventoryUtil.getHolder(var1.getInventory()) instanceof ShopInventory) {
         Player var2 = (Player)var1.getPlayer();
         InventoryHolder var3 = InventoryUtil.getHolder(var1.getInventory());
         boolean var4 = backOnClose && !UserManager.getUser(var2).isOpenNewGUI();
         this.plugin.runTaskLater(() -> this.closeInventory(var2, var3, var4), 1L);
      } else if (InventoryUtil.getHolder(var1.getInventory()) instanceof ItemEditor) {
         Player var5 = (Player)var1.getPlayer();
         if (InventoryUtil.getHolder(var1.getInventory()) instanceof FireworkOptions) {
            this.plugin.itemEditor.saveFireworkOptions(var1.getInventory());
         } else if (InventoryUtil.getHolder(var1.getInventory()) instanceof DynamicPricingOptions) {
            this.plugin.itemEditor.saveDynamicPricingOptions(var1.getInventory());
         } else if (InventoryUtil.getHolder(var1.getInventory()) instanceof AvailableOptions) {
            this.plugin.itemEditor.saveUnsavedOptions(var1.getInventory());
         }

         this.plugin.runTaskLater(var5::updateInventory, 1L);
      }
   }

   private void closeInventory(Player var1, InventoryHolder var2, boolean var3) {
      if (var3 && var2 instanceof ShopPage) {
         ShopPage var6 = (ShopPage)var2;
         if (!var6.isDisabledBackButton() || var6.isSubSection() && !BackButton.SUB_SECTIONS) {
            if (var6.isSubSection()) {
               this.plugin.getSection(var6.getRootSection()).openShopSection(var1, var6.isDisabledBackButton());
            } else {
               MainMenu.open(var1);
            }

            this.plugin.getSoundEffects().useBackButton(var1);
         }
      } else if (var3 && var2 instanceof TransactionMenu) {
         TransactionMenu var4 = (TransactionMenu)var2;
         if (var4.getTransactionType() != Transaction.Type.BUY_STACKS_SCREEN) {
            ShopSection var5 = this.plugin.getSection(var4.getShopItem().section);
            var5.openShopSection(var1, var5.getPageForShopItem(var4.getShopItem().itemLoc), var4.isDisabledBackButton());
            this.plugin.getSoundEffects().useBackButton(var1);
         } else {
            new TransactionMenu(
                  var1,
                  var4.getShopItem(),
                  var4.getRootSection(),
                  var4.isDisabledBackButton(),
                  Transaction.Mode.getFromType(var4.getTransactionType()),
                  Transaction.Type.BUY_SCREEN,
                  var4.getAmount()
               )
               .open(false);
         }
      } else if (var2 instanceof SellGUI) {
         ((SellGUI)var2).sellItems(var1);
      }

      var1.updateInventory();
   }

   private void close(Player var1) {
      UserManager.getUser(var1).setOpenNewGUI(true);
      var1.closeInventory();
   }

   private void handleShiftSellGUI(InventoryClickEvent var1, Inventory var2, InventoryHolder var3, int var4) {
      ItemStack var5 = var1.getCurrentItem();
      if (var5 != null) {
         ItemStack var6 = var5.clone();
         ArrayList<Integer> var7 = new ArrayList<>();
         ItemStack[] var8 = var3.getInventory().getContents();

         for (int var9 = 0; var9 < var8.length; var9++) {
            ItemStack var10 = var8[var9];
            if (var10 != null && var10.isSimilar(var6)) {
               int var11 = Math.min(var10.getMaxStackSize(), var6.getMaxStackSize());
               int var12 = var11 - var10.getAmount();
               if (var12 > 0) {
                  int var13 = Math.min(var12, var6.getAmount());
                  var6.setAmount(var6.getAmount() - var13);
                  var7.add(var9);
                  if (var6.getAmount() <= 0) {
                     break;
                  }
               }
            }
         }

         if (var6.getAmount() > 0) {
            for (int var14 = 0; var14 < var8.length; var14++) {
               if (var8[var14] == null) {
                  int var16 = Math.min(var6.getAmount(), var6.getMaxStackSize());
                  var6.setAmount(var6.getAmount() - var16);
                  var7.add(var14);
                  if (var6.getAmount() <= 0) {
                     break;
                  }
               }
            }
         }

         for (int var17 : var7) {
            if (this.plugin.navBar.isEnableSellGUINav() && var17 >= var2.getSize() - 9 && var17 <= var2.getSize() - 1) {
               var1.setCancelled(true);
               return;
            }

            if (SellGUI.isFillItem(var17)) {
               var1.setCancelled(true);
               return;
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH,
      ignoreCancelled = true
   )
   public void onDrag(InventoryDragEvent var1) {
      InventoryHolder var2 = InventoryUtil.getHolder(var1.getInventory());
      if (var2 instanceof ShopInventory || var2 instanceof ItemEditor) {
         int var3 = var2.getInventory().getSize();

         for (int var5 : var1.getRawSlots()) {
            if (var5 < var3
               && (!(var2 instanceof SellGUI) || SellGUI.isFillItem(var5) || this.plugin.navBar.isEnableSellGUINav() && var5 >= var3 - 9 && var5 <= var3 - 1)) {
               var1.setCancelled(true);
               var1.setResult(Result.DENY);
               return;
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.HIGH
   )
   public void onCreative(InventoryCreativeEvent var1) {
      InventoryHolder var2 = InventoryUtil.getHolder(var1.getInventory());
      if (var2 instanceof ShopInventory || var2 instanceof ItemEditor) {
         var1.setCancelled(true);
         var1.setResult(Result.DENY);
      }
   }

   public boolean purchaseItem(Player var1, ShopItem var2) {
      int var3 = var2.getStackSize();
      double var4 = 0.0;
      if (!PlayerItems.canBuy(var1, var2)) {
         SendMessage.chatToPlayer(var1, Lang.MORE_SPACE_NEEDED.get());
         this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NO_INVENTORY_SPACE);
         return false;
      } else {
         var3 = PlayerItems.getFinalAmount(var2, var1, var3);
         if (var2.isMinBuy(var3)) {
            SendMessage.chatToPlayer(var1, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get().replace("%min-buy%", String.valueOf(var2.getMinBuy())));
            this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            if (var2.getLimitedStockMode() != 0) {
               int var6 = this.plugin.getLimitedStockProvider().getItemStock(var2, var1.getUniqueId(), true);
               var3 = Math.min(var3, var6);
               if (var6 <= 0) {
                  SendMessage.chatToPlayer(var1, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(var1);
                  this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NO_ITEM_STOCK_LEFT);
                  return false;
               }
            }

            PreTransactionEvent var10 = new PreTransactionEvent(var2, var1, var3, var2.getBuyPrice(var1, var3), Transaction.Type.QUICK_BUY);
            Bukkit.getPluginManager().callEvent(var10);
            var4 = var10.getPrice();
            if (var10.isCancelled()) {
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.TRANSACTION_CANCELLED);
               return false;
            } else if (!(var4 >= 0.0)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_PURCHASE_ITEM.get());
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NEGATIVE_ITEM_PRICE);
               return false;
            } else if (this.plugin.getEcoHandler().getEcon(var2).getBalance(var1) < var4) {
               SendMessage.chatToPlayer(
                  var1, Lang.INSUFFICIENT_MONEY.get().replace("%currency%", this.plugin.getEcoHandler().getEcon(var2.getEcoType()).getFriendly())
               );
               this.plugin.getSoundEffects().insufficientFunds(var1);
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
               return false;
            } else {
               if (!this.plugin.getEcoHandler().getEcon(var2).withdrawBalance(var1, var4)) {
                  SendMessage.chatToPlayer(
                     var1, Lang.INSUFFICIENT_MONEY.get().replace("%currency%", this.plugin.getEcoHandler().getEcon(var2.getEcoType()).getFriendly())
                  );
                  this.plugin.getSoundEffects().insufficientFunds(var1);
                  this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                  return false;
               }

               if (var2.isBuyCommand() && this.plugin.commandItems) {
                  CommandHelper.executeCommandItem(var2, var1, CommandType.BUY, String.valueOf(var4), var3);
               }

               if (!var2.isBuyCommand() || var2.isGiveItem()) {
                  PlayerItems.giveItems(var1, var2, var3);
               }

               boolean var7 = false;
               if (var2.getLimitedStockMode() != 0) {
                  var7 = true;
                  if (true) {
                     this.plugin.getLimitedStockProvider().buyItems(var2, var1.getUniqueId(), var3);
                  }
               }

               if (this.plugin.dynamicPricing && var2.isDynamicPricing()) {
                  var7 = true;
                  if (true) {
                     this.plugin.getDynamicPricingProvider().buyItem(var2.getItemPath(), var3);
                  }
               }

               SendMessage.sendTransactionMessage(
                  var1, var3, var4, var2, Transaction.Mode.BUY, Transaction.Type.QUICK_BUY, var2.isBuyCommand() && !var2.isGiveItem()
               );
               if (var2.isCloseMenu()) {
                  this.close(var1);
               }

               this.callPost(
                  var2,
                  var1,
                  var3,
                  var4,
                  Transaction.Type.QUICK_BUY,
                  var2.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
               );
               return var7;
            }
         }
      }
   }

   public boolean purchaseItemA(Player var1, ShopItem var2) {
      int var3 = var2.getStackSize();
      HashMap var4 = new HashMap();
      if (!PlayerItems.canBuy(var1, var2)) {
         SendMessage.chatToPlayer(var1, Lang.MORE_SPACE_NEEDED.get());
         this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NO_INVENTORY_SPACE);
         return false;
      } else {
         var3 = PlayerItems.getFinalAmount(var2, var1, var3);
         if (var2.isMinBuy(var3)) {
            SendMessage.chatToPlayer(var1, Lang.NOT_ENOUGH_ITEMS_TO_BUY.get().replace("%min-buy%", String.valueOf(var2.getMinBuy())));
            this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            if (var2.getLimitedStockMode() != 0) {
               int var5 = this.plugin.getLimitedStockProvider().getItemStock(var2, var1.getUniqueId(), true);
               var3 = Math.min(var3, var5);
               if (var5 <= 0) {
                  SendMessage.chatToPlayer(var1, Lang.NOT_ENOUGH_STOCK_AVAILABLE.get());
                  this.plugin.getSoundEffects().insufficientStock(var1);
                  this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_BUY, Transaction.Result.NO_ITEM_STOCK_LEFT);
                  return false;
               }
            }

            PreTransactionEvent var10 = new PreTransactionEvent(
               Collections.singletonMap(var2, var3),
               var2.getABuyPrice().getBuyPrices(null, var1, var3, var2.getBuyModifiers()),
               var1,
               var3,
               Transaction.Type.QUICK_BUY
            );
            Bukkit.getPluginManager().callEvent(var10);
            Map var9 = var10.getPrices();
            if (var10.isCancelled()) {
               this.callPost(var2, var1, var3, var9, Transaction.Type.QUICK_BUY, Transaction.Result.TRANSACTION_CANCELLED);
               return false;
            } else if (!this.arePricesValid(var9)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_PURCHASE_ITEM.get());
               this.callPost(var2, var1, var3, var9, Transaction.Type.QUICK_BUY, Transaction.Result.NEGATIVE_ITEM_PRICE);
               return false;
            } else {
               Map var6 = this.plugin.getEcoHandler().has(var9, var1);
               if (var6.size() != 0) {
                  SendMessage.chatToPlayer(
                     var1,
                     Lang.INSUFFICIENT_MONEY
                        .get()
                        .replace(
                           "%currency%",
                           this.plugin
                              .formatPrices(Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0], var6.keySet(), var2.getABuyPrice().isAll())
                        )
                  );
                  this.plugin.getSoundEffects().insufficientFunds(var1);
                  this.callPost(var2, var1, var3, var9, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                  return false;
               } else {
                  if (!this.plugin.getEcoHandler().withdraw(var9, var1)) {
                     SendMessage.chatToPlayer(
                        var1,
                        Lang.INSUFFICIENT_MONEY
                           .get()
                           .replace(
                              "%currency%",
                              this.plugin.formatPrices(
                                 Lang.INSUFFICIENT_MONEY.get().getLegacy().split("%currency%")[0], var9.keySet(), var2.getABuyPrice().isAll()
                              )
                           )
                     );
                     this.plugin.getSoundEffects().insufficientFunds(var1);
                     this.callPost(var2, var1, var3, var9, Transaction.Type.QUICK_BUY, Transaction.Result.INSUFFICIENT_FUNDS);
                     return false;
                  }

                  if (var2.isBuyCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(var2, var1, CommandType.BUY, var9.toString(), var3);
                  }

                  if (!var2.isBuyCommand() || var2.isGiveItem()) {
                     PlayerItems.giveItems(var1, var2, var3);
                  }

                  boolean var7 = false;
                  if (var2.getLimitedStockMode() != 0) {
                     var7 = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().buyItems(var2, var1.getUniqueId(), var3);
                     }
                  }

                  if (this.plugin.dynamicPricing && var2.isDynamicPricing()) {
                     var7 = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().buyItem(var2.getItemPath(), var3);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     var1, var3, var9, var2, Transaction.Mode.BUY, Transaction.Type.QUICK_BUY, var2.isBuyCommand() && !var2.isGiveItem()
                  );
                  if (var2.isCloseMenu()) {
                     this.close(var1);
                  }

                  this.callPost(
                     var2,
                     var1,
                     var3,
                     var9,
                     Transaction.Type.QUICK_BUY,
                     var2.isBuyCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return var7;
               }
            }
         }
      }
   }

   public boolean sellItem(Player var1, ShopItem var2, int var3) {
      double var4 = 0.0;
      if (!PlayerItems.canSell(var2, var1)) {
         SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get());
         this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.NO_ITEMS_FOUND);
         return false;
      } else {
         if (var2.getLimitedSellMode() != 0) {
            int var6 = this.plugin.getLimitedSellProvider().getItemLimit(var2, var1.getUniqueId(), true);
            var3 = Math.min(var3, var6);
            if (var3 <= 0) {
               SendMessage.chatToPlayer(var1, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.REACHED_SELL_LIMIT);
               return false;
            }
         }

         ArrayList<ItemStack> var12 = PlayerItems.getSellAble(var2, var1, var3);
         var3 = var12.stream().mapToInt(ItemStack::getAmount).sum();
         if (var2.isMinSell(var3)) {
            SendMessage.chatToPlayer(
               var1, Lang.NOT_ENOUGH_ITEMS_TO_SELL.get().replace("%min-sell%", String.valueOf(var2.getMinSell())).replace("%quantity%", String.valueOf(var3))
            );
            this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            var4 = var12.stream().mapToDouble(var2x -> var2.getSellPrice(var1, var2x)).sum();
            if (var2.getEcoType().getType() == EconomyType.ITEM && !this.canPay(var1, var2.getEcoType(), var4)) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get());
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.CANT_STORE_PAYMENT);
               return false;
            } else {
               PreTransactionEvent var7 = new PreTransactionEvent(var2, var1, var3, var4, Transaction.Type.QUICK_SELL);
               Bukkit.getPluginManager().callEvent(var7);
               var4 = var7.getPrice();
               if (var7.isCancelled()) {
                  this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.TRANSACTION_CANCELLED);
                  return false;
               } else {
                  this.plugin.getEcoHandler().getEcon(var2.getEcoType()).depositBalance(var1, var4);
                  if (var2.isSellCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(var2, var1, CommandType.SELL, String.valueOf(var4), var3);
                  }

                  if (!var2.isSellCommand() || var2.isRequireItem()) {
                     var12.forEach(var1x -> var1.getInventory().removeItem(new ItemStack[]{var1x}));
                  }

                  boolean var8 = false;
                  if (var2.getLimitedSellMode() != 0) {
                     var8 = true;
                     if (true) {
                        this.plugin.getLimitedSellProvider().sellItems(var2, var1.getUniqueId(), var3, false);
                     }
                  }

                  if (var2.isRefillStock()) {
                     var8 = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().sellItems(var2, var1.getUniqueId(), var3);
                     }
                  }

                  if (this.plugin.dynamicPricing && var2.isDynamicPricing()) {
                     var8 = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().sellItem(var2.getItemPath(), var3);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     var1, var3, var4, var2, Transaction.Mode.SELL, Transaction.Type.QUICK_SELL, var2.isSellCommand() && !var2.isRequireItem()
                  );
                  if (var2.isCloseMenu()) {
                     this.close(var1);
                  }

                  this.callPost(
                     var2,
                     var1,
                     var3,
                     var4,
                     Transaction.Type.QUICK_SELL,
                     var2.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return var8;
               }
            }
         }
      }
   }

   public boolean sellItemA(Player var1, ShopItem var2, int var3) {
      HashMap<EcoType, Double> var4 = new HashMap<>();
      if (!PlayerItems.canSell(var2, var1)) {
         SendMessage.chatToPlayer(var1, Lang.NO_ITEM_FOUND.get());
         this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.NO_ITEMS_FOUND);
         return false;
      } else {
         if (var2.getLimitedSellMode() != 0) {
            int var5 = this.plugin.getLimitedSellProvider().getItemLimit(var2, var1.getUniqueId(), true);
            var3 = Math.min(var3, var5);
            if (var3 <= 0) {
               SendMessage.chatToPlayer(var1, Lang.LIMITED_ITEM_SELL_AMOUNT.get());
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.REACHED_SELL_LIMIT);
               return false;
            }
         }

         ArrayList<ItemStack> var12 = PlayerItems.getSellAble(var2, var1, var3);
         var3 = var12.stream().mapToInt(ItemStack::getAmount).sum();
         if (var2.isMinSell(var3)) {
            SendMessage.chatToPlayer(
               var1, Lang.NOT_ENOUGH_ITEMS_TO_SELL.get().replace("%min-sell%", String.valueOf(var2.getMinSell())).replace("%quantity%", String.valueOf(var3))
            );
            this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.NOT_ENOUGH_ITEMS);
            return false;
         } else {
            for (ItemStack var7 : var12) {
               for (Entry var9 : var2.getASellPrice().getSellPrices(null, var1, var7, var2.getSellModifiers()).entrySet()) {
                  var4.put((EcoType)var9.getKey(), var4.getOrDefault(var9.getKey(), 0.0) + (Double)var9.getValue());
               }
            }

            if (var4.entrySet()
               .stream()
               .anyMatch(
                  var2x -> ((EcoType)var2x.getKey()).getType() == EconomyType.ITEM && !this.canPay(var1, (EcoType)var2x.getKey(), (Double)var2x.getValue())
               )) {
               SendMessage.chatToPlayer(var1, Lang.CANNOT_STORE_PAYMENT.get());
               this.callPost(var2, var1, var3, var4, Transaction.Type.QUICK_SELL, Transaction.Result.CANT_STORE_PAYMENT);
               return false;
            } else {
               PreTransactionEvent var13 = new PreTransactionEvent(Collections.singletonMap(var2, var3), var4, var1, var3, Transaction.Type.QUICK_BUY);
               Bukkit.getPluginManager().callEvent(var13);
               Map var11 = var13.getPrices();
               if (var13.isCancelled()) {
                  this.callPost(var2, var1, var3, var11, Transaction.Type.QUICK_SELL, Transaction.Result.TRANSACTION_CANCELLED);
                  return false;
               } else {
                  this.plugin.getEcoHandler().deposit(var11, var1);
                  if (var2.isSellCommand() && this.plugin.commandItems) {
                     CommandHelper.executeCommandItem(var2, var1, CommandType.SELL, var11.toString(), var3);
                  }

                  if (!var2.isSellCommand() || var2.isRequireItem()) {
                     var12.forEach(var1x -> var1.getInventory().removeItem(new ItemStack[]{var1x}));
                  }

                  boolean var14 = false;
                  if (var2.getLimitedSellMode() != 0) {
                     var14 = true;
                     if (true) {
                        this.plugin.getLimitedSellProvider().sellItems(var2, var1.getUniqueId(), var3, false);
                     }
                  }

                  if (var2.isRefillStock()) {
                     var14 = true;
                     if (true) {
                        this.plugin.getLimitedStockProvider().sellItems(var2, var1.getUniqueId(), var3);
                     }
                  }

                  if (this.plugin.dynamicPricing && var2.isDynamicPricing()) {
                     var14 = true;
                     if (true) {
                        this.plugin.getDynamicPricingProvider().sellItem(var2.getItemPath(), var3);
                     }
                  }

                  SendMessage.sendTransactionMessage(
                     var1, var3, var11, var2, Transaction.Mode.SELL, Transaction.Type.QUICK_SELL, var2.isSellCommand() && !var2.isRequireItem()
                  );
                  if (var2.isCloseMenu()) {
                     this.close(var1);
                  }

                  this.callPost(
                     var2,
                     var1,
                     var3,
                     var11,
                     Transaction.Type.QUICK_SELL,
                     var2.isSellCommand() ? Transaction.Result.SUCCESS_COMMANDS_EXECUTED : Transaction.Result.SUCCESS
                  );
                  return var14;
               }
            }
         }
      }
   }

   private void callPost(ShopItem var1, Player var2, int var3, double var4, Transaction.Type var6, Transaction.Result var7) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(var1, var2, var3, var4, var6, var7));
   }

   private void callPost(ShopItem var1, Player var2, int var3, Map<EcoType, Double> var4, Transaction.Type var5, Transaction.Result var6) {
      Bukkit.getPluginManager().callEvent(new PostTransactionEvent(Collections.singletonMap(var1, var3), var4, var2, var3, var5, var6));
   }

   private String getCommand(String var1, ShopItem var2, int var3, double var4) {
      return var1.replace("%quantity%", String.valueOf(var3))
         .replace("%price%", this.plugin.formatPrice(var2.getEcoType(), var4))
         .replace("%buyPrice%", this.plugin.formatPrice(var2.getEcoType(), var2.getBuyPrice(var3)))
         .replace("%sellPrice%", this.plugin.formatPrice(var2.getEcoType(), var2.getSellPrice()));
   }
}
