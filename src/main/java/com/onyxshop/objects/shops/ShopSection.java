package com.onyxshop.objects.shops;

import java.util.Collection;
import java.util.List;
import com.onyxshop.objects.DisplayItem;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopPageItems;
import com.onyxshop.objects.mappings.ClickAction;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface ShopSection {
   Collection<DisplayItem> getDisplayItems();

   DisplayItem getDisplayItem(String itemId);

   boolean isActionItem(String itemId, String text);

   Collection<ShopItem> getShopItems();

   ShopItem getShopItem(String shopId);

   String getSection();

   ShopPageItems getShopPageItems(int page);

   int getPages();

   long getUpdateInterval();

   boolean isSubSection();

   boolean isHidden();

   boolean isCommandItem();

   List<String> getClickCommands();

   boolean isCloseMenu();

   ShopType getType();

   List<String> getItemLocs();

   List<String> getShopItemLocs();

   ClickAction getClickAction(ClickType clickType);

   int getPageForShopItem(String shopId);

   List<ShopItem> getPageItems(int page);

   void openShopSection(Player player, boolean flag);

   void openShopSection(Player player, boolean flag, String shopId);

   void openShopSection(Player player, int value, boolean flag);

   void openShopSection(Player player, int value, boolean flag, @Nullable String shopId);

   ItemStack updateItem(Player player, ShopItem shopItem);

   void reloadItem(String itemId);

   void addNewItem(String itemId);
}
