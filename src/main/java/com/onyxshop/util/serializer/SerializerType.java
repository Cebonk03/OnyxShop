package com.onyxshop.util.serializer;

public enum SerializerType {
   BUKKIT,
   BUKKIT_V2,
   ADVENTURE,
   CUSTOM_V1,
   CUSTOM_V2;

   final SerializerType type = this;

   private SerializerType() {
   }

   public boolean isBukkit() {
      return this == BUKKIT || this == BUKKIT_V2;
   }
}
