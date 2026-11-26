package com.onyxshop.api.prices;

import java.util.List;
import java.util.Map;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.priceModifiers.Modifiers;
import com.onyxshop.util.EcoType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class AdvancedSellPrice {
   private final com.onyxshop.objects.prices.AdvancedSellPrice aPrices;

   public AdvancedSellPrice(ShopItem var1) {
      this.aPrices = var1.getASellPrice();
   }

   public List<EcoType> getSellTypes() {
      return this.aPrices.getTypes();
   }

   public boolean isSellAble() {
      return this.aPrices.isSellAble();
   }

   public boolean giveAll() {
      return this.aPrices.isAll();
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType var1, ItemStack var2) {
      return this.aPrices.getSellPrices(var1, var2);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType var1, ItemStack var2, int var3, int var4) {
      return this.aPrices.getSellPrices(var1, var2, var3, var4);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType var1, @Nullable Modifiers var2, Player var3, ItemStack var4) {
      return this.aPrices.getSellPrices(var1, var3, var4, var2);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType var1, @Nullable Modifiers var2, Player var3, ItemStack var4, int var5, int var6) {
      return this.aPrices.getSellPrices(var1, var3, var4, var5, var6, var2);
   }
}
