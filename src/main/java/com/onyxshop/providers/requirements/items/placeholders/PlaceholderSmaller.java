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

public class PlaceholderSmaller extends PlaceholderRequirement {
   private final Object value;
   private final boolean staticValue;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public PlaceholderSmaller(PlaceholderRequirement.ComparisonOperator var1, String var2, List<String> var3, String var4) throws ItemRequirementLoadException {
      super(var2.split(var1.symbol)[0]);
      Pattern var5 = Pattern.compile("%([^% ]+)%");
      this.staticValue = !var5.matcher(var2.split(var1.symbol)[1]).find();
      if (this.staticValue) {
         this.value = this.getNumber(var2.split(var1.symbol)[1]);
      } else {
         this.value = var2.split(var1.symbol)[1];
      }

      this.lore = RequirementManager.getLore(var3, Lang.PLACEHOLDER_SMALLER_REQUIREMENT_LORE.get().toString());
      this.customMSG = var4;
   }

   @Override
   public boolean isMet(Player var1) {
      try {
         return this.getNumber(this.getFormatted(var1)).compareTo(this.staticValue ? (BigDecimal)this.value : this.getNumber(this.getValue(var1))) <= 0;
      } catch (ItemRequirementLoadException var3) {
         SendMessage.errorMessage(var3.getMessage());
         return false;
      }
   }

   private String getValue(Player var1) {
      return this.staticValue ? this.value.toString() : PlaceholderAPIHook.translate(var1, this.value.toString());
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3, "%requirement%", this.getValue(var1), "%progress%", this.getFormatted(var1));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLACEHOLDER_SMALLER_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(
            var1, var2.replace("%requirement%", this.staticValue ? this.value.toString() : this.getValue(var1)).replace("%progress%", this.getFormatted(var1))
         );
      }
   }

   private BigDecimal getNumber(String var1) throws ItemRequirementLoadException {
      if (var1.contains("\\.") || var1.contains(",")) {
         try {
            return BigDecimal.valueOf(Double.parseDouble(var1)).stripTrailingZeros();
         } catch (NumberFormatException var4) {
         }
      }

      try {
         return new BigDecimal(var1);
      } catch (NumberFormatException var3) {
         throw new ItemRequirementLoadException("Invalid placeholder value for " + var1 + ", needs to be a number for PLACEHOLDER_SMALLER requirement!");
      }
   }
}
