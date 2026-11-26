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

   public PlaceholderRequirement(String var1) throws ItemRequirementLoadException {
      Pattern var2 = Pattern.compile("%([^% ]+)%");
      if (!var2.matcher(var1).find()) {
         throw new ItemRequirementLoadException("Invalid placeholder for " + var1 + ", should start and end with a percent symbol");
      } else {
         this.placeholder = var1;
      }
   }

   public static PlaceholderRequirement initPlaceholderComparison(RequirementType var0, String var1, List<String> var2, String var3) throws ItemRequirementLoadException {
      PlaceholderRequirement.ComparisonOperator var4 = PlaceholderRequirement.ComparisonOperator.fromType(var0, var1);
      if (var4 == null) {
         throw new ItemRequirementLoadException("Invalid placeholder requirement, cannot determine comparison operator from value '" + var1 + "'");
      } else if (!var1.contains(var4.symbol)) {
         throw new ItemRequirementLoadException("Invalid format for placeholder comparison, should be %placeholder%" + var4.symbol + "<value>");
      } else {
         switch (var4) {
            case EQUALS:
               return new PlaceholderEquals(var4, var1, var2, var3);
            case DIFFERS:
               return new PlaceholderDiffers(var4, var1, var2, var3);
            case LARGER:
               return new PlaceholderLarger(var4, var1, var2, var3);
            case SMALLER:
               return new PlaceholderSmaller(var4, var1, var2, var3);
            default:
               throw new ItemRequirementLoadException(Lang.INVALID_ITEM_REQUIREMENT_TYPE.get().replace("%type%", var0.name()));
         }
      }
   }

   public String getFormatted(Player var1) {
      return PlaceholderAPIHook.translate(var1, this.placeholder);
   }

   @Override
   public boolean isMet(Player var1) {
      return false;
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return new String[0];
   }

   @Override
   public void sendNotMetMessage(Player var1) {
   }

   public static enum ComparisonOperator {
      EQUALS("=="),
      DIFFERS("!="),
      LARGER(">="),
      SMALLER("<=");

      public String symbol;

      private ComparisonOperator(String nullxx) {
         this.symbol = nullxx;
      }

      public static PlaceholderRequirement.ComparisonOperator fromType(RequirementType var0, String var1) {
         switch (var0) {
            case PLACEHOLDER:
               return fromOperator(var1);
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

      public static PlaceholderRequirement.ComparisonOperator fromOperator(String var0) {
         for (PlaceholderRequirement.ComparisonOperator var4 : values()) {
            if (var0.contains(var4.symbol)) {
               return var4;
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
