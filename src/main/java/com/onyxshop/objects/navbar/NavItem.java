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

   boolean isStatic(int var1, int var2);

   NavBar.ItemAction getAction();

   NavBar.ItemAction getAction(int var1, int var2);

   List<String> getCommands();

   List<String> getCommands(int var1);

   ItemStack getItem(Player var1);

   ItemStack getItem(Player var1, EcoType var2, int var3, int var4);

   ItemStack getItem(Player var1, Transaction.Mode var2, ShopItem var3, EcoType var4);

   ItemStack getItem(Player var1, EcoType var2, ShopSearch.QueryMethod var3, int var4, int var5);

   String getBedrockTexture();

   String getBedrockTexture(int var1, int var2);
}
