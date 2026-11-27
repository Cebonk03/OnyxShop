package com.onyxshop.providers.requirements.items.placeholders;

import java.util.List;
import java.util.regex.Pattern;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.PlaceholderAPIHook;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.providers.requirements.items.PlaceholderRequirement;
import org.bukkit.entity.Player;

public class PlaceholderDiffers extends PlaceholderRequirement {
   private final String value;
   private final boolean staticValue;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public PlaceholderDiffers(PlaceholderRequirement.ComparisonOperator comparisonOperator, String itemId, List<String> values, String text) throws ItemRequirementLoadException {
      super(itemId.split(comparisonOperator.symbol)[0]);
      this.value = itemId.split(comparisonOperator.symbol)[1];
      Pattern pattern = Pattern.compile("%([^% ]+)%");
      this.staticValue = !pattern.matcher(this.value).find();
      this.lore = RequirementManager.getLore(values, Lang.PLACEHOLDER_DIFFERS_REQUIREMENT_LORE.get().toString());
      this.customMSG = text;
   }

   @Override
   public boolean isMet(Player player) {
      return !this.getFormatted(player).equals(this.getValue(player));
   }

   private String getValue(Player player) {
      return this.staticValue ? this.value : PlaceholderAPIHook.translate(player, this.value);
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2, "%requirement%", this.getValue(player), "%progress%", this.getFormatted(player));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLACEHOLDER_DIFFERS_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(player, text.replace("%requirement%", this.getValue(player)).replace("%progress%", this.getFormatted(player)));
      }
   }
}
