package com.onyxshop.api.objects;

import java.util.Map;
import com.onyxshop.util.EcoType;
import com.onyxshop.util.Transaction;

public class TransactionResult {
   private final Transaction.Result result;
   private final Map<EcoType, Double> prices;
   private final int amount;

   public TransactionResult(Transaction.Result var1) {
      this.result = var1;
      this.prices = null;
      this.amount = 0;
   }

   public TransactionResult(Transaction.Result var1, Map<EcoType, Double> var2, int var3) {
      this.result = var1;
      this.prices = var2;
      this.amount = var3;
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
