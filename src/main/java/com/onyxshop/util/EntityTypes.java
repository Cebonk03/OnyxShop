package com.onyxshop.util;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.EntityType;

public class EntityTypes {
   public EntityTypes() {
   }

   public static Optional<EntityType> matchEntityType(String var0) {
      var0 = var0.replace(" ", "_").toUpperCase(Locale.ENGLISH);
      if (ServerInfo.supportsComponents()) {
         switch (var0) {
            case "MUSHROOM_COW":
               var0 = "MOOSHROOM";
               break;
            case "SNOWMAN":
               var0 = "SNOW_GOLEM";
         }
      }

      try {
         return Optional.of(EntityType.valueOf(var0));
      } catch (IllegalArgumentException var3) {
         return Optional.empty();
      }
   }
}
