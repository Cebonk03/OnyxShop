package com.onyxshop.providers.requirements.items;

import java.util.List;
import java.util.regex.Pattern;
import com.onyxshop.files.Lang;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.requirements.RequirementType;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderDiffers;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderEquals;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderLarger;
import com.onyxshop.providers.requirements.items.placeholders.PlaceholderSmaller;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class PlaceholderRequirement implements com.onyxshop.providers.requirements.ItemRequirement {
   private final String placeholder;

   public PlaceholderRequirement(String itemId) throws ItemRequirementLoadException {
      Pattern pattern = Pattern.compile("%([^% ]+)%");
      if (!pattern.matcher(itemId).find()) {
         throw new ItemRequirementLoadException("Invalid placeholder for " + itemId + ", should start and end with a percent symbol");
      } else {
         this.placeholder = itemId;
      }
   }

   public static PlaceholderRequirement initPlaceholderComparison(RequirementType requirementType, String type, List<String> itemIds, String text) throws ItemRequirementLoadException {
      PlaceholderRequirement.ComparisonOperator comparisonOperator = PlaceholderRequirement.ComparisonOperator.fromType(requirementType, type);
      if (comparisonOperator == null) {
         throw new ItemRequirementLoadException("Invalid placeholder requirement, cannot determine comparison operator from value '" + type + "'");
      } else if (!type.contains(comparisonOperator.symbol)) {
         throw new ItemRequirementLoadException("Invalid format for placeholder comparison, should be %placeholder%" + comparisonOperator.symbol + "<value>");
      } else {
         switch (comparisonOperator) {
            case EQUALS:
               return new PlaceholderEquals(comparisonOperator, type, itemIds, text);
            case DIFFERS:
               return new PlaceholderDiffers(comparisonOperator, type, itemIds, text);
            case LARGER:
               return new PlaceholderLarger(comparisonOperator, type, itemIds, text);
            case SMALLER:
               return new PlaceholderSmaller(comparisonOperator, type, itemIds, text);
            default:
               throw new ItemRequirementLoadException(Lang.INVALID_ITEM_REQUIREMENT_TYPE.get().replace("%type%", requirementType.name()));
         }
      }
   }

   public String getFormatted(Player player) {
      return PlaceholderAPIHook.translate(player, this.placeholder);
   }

   @Override
   public boolean isMet(Player player) {
      return false;
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return new String[0];
   }

   @Override
   public void sendNotMetMessage(Player player) {
   }

   public static enum ComparisonOperator {
      EQUALS("=="),
      DIFFERS("!="),
      LARGER(">="),
      SMALLER("<=");

      public String symbol;

      private ComparisonOperator(String text) {
         this.symbol = text;
      }

      public static PlaceholderRequirement.ComparisonOperator fromType(RequirementType requirementType, String type) {
         switch (requirementType) {
            case PLACEHOLDER:
               return fromOperator(type);
            case PLACEHOLDER_EQUALS:
               return EQUALS;
            case PLACEHOLDER_DIFFERS:
               return DIFFERS;
            case PLACEHOLDER_LARGER:
               return LARGER;
            case PLACEHOLDER_SMALLER:
               return SMALLER;
            default:
               return null;
         }
      }

      public static PlaceholderRequirement.ComparisonOperator fromOperator(String text) {
         for (PlaceholderRequirement.ComparisonOperator comparisonOperator : values()) {
            if (text.contains(comparisonOperator.symbol)) {
               return comparisonOperator;
            }
         }

         return null;
      }

      public RequirementType getType() {
         switch (this) {
            case EQUALS:
               return RequirementType.PLACEHOLDER_EQUALS;
            case DIFFERS:
               return RequirementType.PLACEHOLDER_DIFFERS;
            case LARGER:
               return RequirementType.PLACEHOLDER_LARGER;
            case SMALLER:
               return RequirementType.PLACEHOLDER_SMALLER;
            default:
               return RequirementType.PLACEHOLDER;
         }
      }
   }
}
