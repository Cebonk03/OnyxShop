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

   public void addIndex(int index) {
      this.indexes.add(index);
   }

   public Sold addValues(int value, Map<EcoType, Double> map) {
      this.amount = this.amount + value;
      map.forEach((key, value2) -> this.prices.merge(key, value2, Double::sum));
      return this;
   }

   public Sold addValue(Sold sold) {
      this.amount = this.amount + sold.getAmount();
      sold.getPrices().forEach((key, value) -> this.prices.merge(key, value, Double::sum));
      return this;
   }

   public Sold addAmount(int amount) {
      this.amount = this.amount + amount;
      return this;
   }

   public Sold addPrice(Map<EcoType, Double> prices) {
      prices.forEach((key, value) -> this.prices.put(key, this.prices.getOrDefault(key, 0.0) + value));
      return this;
   }
}
