package com.onyxshop.util;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public class EcoType {
   private final EconomyType type;
   private final String currency;

   public EcoType(EconomyType economyType) {
      this.type = economyType;
      this.currency = null;
   }

   public EcoType(EconomyType economyType, String type) {
      this.type = economyType;
      this.currency = type;
   }

   public EconomyType getType() {
      return this.type;
   }

   @Nullable
   public String getCurrency() {
      return this.currency;
   }

   @Override
   public String toString() {
      return this.currency == null ? this.type.name() : this.type.name() + ":" + this.currency;
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value != null && this.getClass() == value.getClass()) {
         EcoType ecoType = (EcoType)value;
         return this.type.equals(ecoType.type) && Objects.equals(this.currency, ecoType.currency);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.type, this.currency);
   }
}
