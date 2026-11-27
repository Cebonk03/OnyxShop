package com.onyxshop.api.objects;

import java.util.Map;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;

public class TransactionResult {
   private final Transaction.Result result;
   private final Map<EcoType, Double> prices;
   private final int amount;

   public TransactionResult(Transaction.Result result) {
      this.result = result;
      this.prices = null;
      this.amount = 0;
   }

   public TransactionResult(Transaction.Result result, Map<EcoType, Double> map, int value) {
      this.result = result;
      this.prices = map;
      this.amount = value;
   }

   public Transaction.Result getResult() {
      return this.result;
   }

   public Map<EcoType, Double> getPrices() {
      return this.prices;
   }

   public int getAmount() {
      return this.amount;
   }
}
