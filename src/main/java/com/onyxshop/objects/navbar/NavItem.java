package com.onyxshop.objects.navbar;

import java.util.List;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.objects.ShopSearch;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NavItem {
   boolean isDisabled();

   boolean isCloseMenu();

   boolean isOnlyGeyser();

   boolean isStatic(int value, int value2);

   NavBar.ItemAction getAction();

   NavBar.ItemAction getAction(int value, int value2);

   List<String> getCommands();

   List<String> getCommands(int value);

   ItemStack getItem(Player player);

   ItemStack getItem(Player player, EcoType ecoType, int value, int value2);

   ItemStack getItem(Player player, Transaction.Mode mode, ShopItem shopItem, EcoType ecoType);

   ItemStack getItem(Player player, EcoType ecoType, ShopSearch.QueryMethod queryMethod, int value, int value2);

   String getBedrockTexture();

   String getBedrockTexture(int value, int value2);
}
