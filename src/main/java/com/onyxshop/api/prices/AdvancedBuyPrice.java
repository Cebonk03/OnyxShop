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

   public AdvancedBuyPrice(ShopItem shopItem) {
      this.aPrices = shopItem.getABuyPrice();
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

   public Map<EcoType, Double> getBuyPrices(@Nullable EcoType ecoType, int value) {
      return this.aPrices.getBuyPrices(ecoType, value);
   }

   public Map<EcoType, Double> getBuyPrices(@Nullable EcoType ecoType, @Nullable Modifiers modifiers, Player player, int value) {
      return this.aPrices.getBuyPrices(ecoType, player, value, modifiers);
   }
}
