package com.onyxshop.util;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.EntityType;

public class EntityTypes {
   public EntityTypes() {
   }

   public static Optional<EntityType> matchEntityType(String type) {
      type = type.replace(" ", "_").toUpperCase(Locale.ENGLISH);
      if (ServerInfo.supportsComponents()) {
         switch (type) {
            case "MUSHROOM_COW":
               type = "MOOSHROOM";
               break;
            case "SNOWMAN":
               type = "SNOW_GOLEM";
         }
      }

      try {
         return Optional.of(EntityType.valueOf(type));
      } catch (IllegalArgumentException exception) {
         return Optional.empty();
      }
   }
}
