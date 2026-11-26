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

   DisplayItem getDisplayItem(String var1);

   boolean isActionItem(String var1, String var2);

   Collection<ShopItem> getShopItems();

   ShopItem getShopItem(String var1);

   String getSection();

   ShopPageItems getShopPageItems(int var1);

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

   ClickAction getClickAction(ClickType var1);

   int getPageForShopItem(String var1);

   List<ShopItem> getPageItems(int var1);

   void openShopSection(Player var1, boolean var2);

   void openShopSection(Player var1, boolean var2, String var3);

   void openShopSection(Player var1, int var2, boolean var3);

   void openShopSection(Player var1, int var2, boolean var3, @Nullable String var4);

   ItemStack updateItem(Player var1, ShopItem var2);

   void reloadItem(String var1);

   void addNewItem(String var1);
}
