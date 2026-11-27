package com.onyxshop.util;

import com.onyxshop.util.serializer.SerializerType;

public class ItemType {
   public final ItemProviderType providerType;

   public ItemType(ItemProviderType itemProviderType) {
      this.providerType = itemProviderType;
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
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else if (value instanceof ItemProviderType) {
         return value == this.providerType;
      } else if (value instanceof ItemType) {
         ItemType itemType = (ItemType)value;
         return itemType.providerType == this.providerType;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.providerType.hashCode();
   }
}
