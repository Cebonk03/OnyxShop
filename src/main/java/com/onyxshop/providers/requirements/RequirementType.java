package com.onyxshop.providers.requirements;

import com.onyxshop.providers.requirements.items.LevelRequirement;
import com.onyxshop.providers.requirements.items.PermissionRequirement;
import com.onyxshop.providers.requirements.items.PlaceholderRequirement;
import com.onyxshop.providers.requirements.items.PlaytimeRequirement;
import com.onyxshop.providers.requirements.items.TimeRequirement;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderDiffers;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderEquals;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderLarger;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderSmaller;
import com.onyxshop.providers.requirements.items.quest.QuestRequirement;
import com.onyxshop.providers.requirements.items.region.RegionRequirement;

public enum RequirementType {
   PLAYTIME(PlaytimeRequirement.class, "playtime-requirement-not-met"),
   LEVEL(LevelRequirement.class, "level-requirement-not-met"),
   PLACEHOLDER_EQUALS(PlaceholderEquals.class, "placeholder-equals-requirement-not-met"),
   PLACEHOLDER_DIFFERS(PlaceholderDiffers.class, "placeholder-differs-requirement-not-met"),
   PLACEHOLDER_LARGER(PlaceholderLarger.class, "placeholder-larger-requirement-not-met"),
   PLACEHOLDER_SMALLER(PlaceholderSmaller.class, "placeholder-smaller-requirement-not-met"),
   PLACEHOLDER(PlaceholderRequirement.class, ""),
   TIME(TimeRequirement.class, "time-requirement-not-met"),
   QUEST(QuestRequirement.class, "quest-requirement-not-met"),
   PERMISSION(PermissionRequirement.class, "permission-requirement-not-met"),
   REGION(RegionRequirement.class, "region-requirement-not-met"),
   ITEM(com.onyxshop.providers.requirements.items.ItemRequirement.class, "item-requirement-not-met");

   private final String lorePath;
   private final Class<? extends ItemRequirement> clazz;

   private RequirementType(Class<? extends ItemRequirement> nullxx, String nullxxx) {
      this.clazz = nullxx;
      this.lorePath = "display-lore-layout.item-requirements." + nullxxx;
   }

   public Class<? extends ItemRequirement> getClazz() {
      return this.clazz;
   }

   public String getLorePath(String var1) {
      if (this == PLACEHOLDER) {
         PlaceholderRequirement.ComparisonOperator var2 = PlaceholderRequirement.ComparisonOperator.fromOperator(var1);
         if (var2 != null) {
            return var2.getType().lorePath;
         }
      }

      return this.lorePath;
   }
}
