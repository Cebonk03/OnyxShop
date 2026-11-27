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

   public AdvancedSellPrice(ShopItem shopItem) {
      this.aPrices = shopItem.getASellPrice();
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

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType ecoType, ItemStack itemStack) {
      return this.aPrices.getSellPrices(ecoType, itemStack);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType ecoType, ItemStack itemStack, int value, int value2) {
      return this.aPrices.getSellPrices(ecoType, itemStack, value, value2);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType ecoType, @Nullable Modifiers modifiers, Player player, ItemStack itemStack) {
      return this.aPrices.getSellPrices(ecoType, player, itemStack, modifiers);
   }

   public Map<EcoType, Double> getSellPrices(@Nullable EcoType ecoType, @Nullable Modifiers modifiers, Player player, ItemStack itemStack, int value, int value2) {
      return this.aPrices.getSellPrices(ecoType, player, itemStack, value, value2, modifiers);
   }
}
