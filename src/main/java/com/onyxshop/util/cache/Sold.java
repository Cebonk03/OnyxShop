package com.onyxshop.util.cache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.onyxshop.util.EcoType;

public class Sold {
   private Integer amount = 0;
   private final Map<EcoType, Double> prices = new HashMap<>();
   private final Set<Integer> indexes = new HashSet<>();

   public Sold() {
   }

   public Set<Integer> getIndexes() {
      return this.indexes;
   }

   public Integer getAmount() {
      return this.amount;
   }

   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   public void addIndex(int var1) {
      this.indexes.add(var1);
   }

   public Sold addValues(int var1, Map<EcoType, Double> var2) {
      this.amount = this.amount + var1;
      var2.forEach((var1x, var2x) -> this.prices.merge(var1x, var2x, Double::sum));
      return this;
   }

   public Sold addValue(Sold var1) {
      this.amount = this.amount + var1.getAmount();
      var1.getPrices().forEach((var1x, var2) -> this.prices.merge(var1x, var2, Double::sum));
      return this;
   }

   public Sold addAmount(int var1) {
      this.amount = this.amount + var1;
      return this;
   }

   public Sold addPrice(Map<EcoType, Double> var1) {
      var1.forEach((var1x, var2) -> this.prices.put(var1x, this.prices.getOrDefault(var1x, 0.0) + var2));
      return this;
   }
}
