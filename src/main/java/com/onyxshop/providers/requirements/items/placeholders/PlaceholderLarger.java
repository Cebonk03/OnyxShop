package com.onyxshop.providers.requirements.items.placeholders;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.providers.requirements.items.PlaceholderRequirement;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class PlaceholderLarger extends PlaceholderRequirement {
   private final Object value;
   private final boolean staticValue;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public PlaceholderLarger(PlaceholderRequirement.ComparisonOperator comparisonOperator, String itemId, List<String> values, String text) throws ItemRequirementLoadException {
      super(itemId.split(comparisonOperator.symbol)[0]);
      Pattern pattern = Pattern.compile("%([^% ]+)%");
      this.staticValue = !pattern.matcher(itemId.split(comparisonOperator.symbol)[1]).find();
      if (this.staticValue) {
         this.value = this.getNumber(itemId.split(comparisonOperator.symbol)[1]);
      } else {
         this.value = itemId.split(comparisonOperator.symbol)[1];
      }

      this.lore = RequirementManager.getLore(values, Lang.PLACEHOLDER_LARGER_REQUIREMENT_LORE.get().toString());
      this.customMSG = text;
   }

   @Override
   public boolean isMet(Player player) {
      try {
         return this.getNumber(this.getFormatted(player)).compareTo(this.staticValue ? (BigDecimal)this.value : this.getNumber(this.getValue(player))) >= 0;
      } catch (ItemRequirementLoadException exception) {
         SendMessage.errorMessage(exception.getMessage());
         return false;
      }
   }

   private String getValue(Player player) {
      return this.staticValue ? this.value.toString() : PlaceholderAPIHook.translate(player, this.value.toString());
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2, "%requirement%", this.getValue(player), "%progress%", this.getFormatted(player));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLACEHOLDER_LARGER_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(
            player, text.replace("%requirement%", this.staticValue ? this.value.toString() : this.getValue(player)).replace("%progress%", this.getFormatted(player))
         );
      }
   }

   private BigDecimal getNumber(String itemId) throws ItemRequirementLoadException {
      if (itemId.contains("\\.") || itemId.contains(",")) {
         try {
            return BigDecimal.valueOf(Double.parseDouble(itemId)).stripTrailingZeros();
         } catch (NumberFormatException exception) {
         }
      }

      try {
         return new BigDecimal(itemId);
      } catch (NumberFormatException exception) {
         throw new ItemRequirementLoadException("Invalid placeholder value for " + itemId + ", needs to be a number for PLACEHOLDER_LARGER requirement!");
      }
   }
}
