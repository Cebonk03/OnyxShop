package com.onyxshop.util;

import com.onyxshop.util.serializer.SerializerType;

public class ItemType {
   public final ItemProviderType providerType;

   public ItemType(ItemProviderType var1) {
      this.providerType = var1;
   }

   public String getName() {
      return this.providerType.name;
   }

   public String getPrefix() {
      return this.providerType.prefix;
   }

   public SerializerType getSerializer() {
      return this.providerType.serializer;
   }

   @Override
   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 instanceof ItemProviderType) {
         return var1 == this.providerType;
      } else if (var1 instanceof ItemType) {
         ItemType var2 = (ItemType)var1;
         return var2.providerType == this.providerType;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.providerType.hashCode();
   }
}
