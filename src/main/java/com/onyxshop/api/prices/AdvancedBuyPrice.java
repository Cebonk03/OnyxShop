package com.onyxshop.api.prices;

import java.util.List;
import java.util.Map;
import com.onyxshop.objects.ShopItem;
import com.onyxshop.providers.priceModifiers.Modifiers;
import com.onyxshop.util.EcoType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AdvancedBuyPrice {
   private final com.onyxshop.objects.prices.AdvancedBuyPrice aPrices;

   public AdvancedBuyPrice(ShopItem var1) {
      this.aPrices = var1.getABuyPrice();
   }

   public List<EcoType> getBuyTypes() {
      return this.aPrices.getTypes();
   }

   public boolean isBuyAble() {
      return this.aPrices.isBuyAble();
   }

   public boolean requireAll() {
      return this.aPrices.isAll();
   }

   public Map<EcoType, Double> getBuyPrices(@Nullable EcoType var1, int var2) {
      return this.aPrices.getBuyPrices(var1, var2);
   }

   public Map<EcoType, Double> getBuyPrices(@Nullable EcoType var1, @Nullable Modifiers var2, Player var3, int var4) {
      return this.aPrices.getBuyPrices(var1, var3, var4, var2);
   }
}
