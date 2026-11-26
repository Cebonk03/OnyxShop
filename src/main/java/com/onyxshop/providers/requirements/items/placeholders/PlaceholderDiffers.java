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

   public PlaceholderDiffers(PlaceholderRequirement.ComparisonOperator var1, String var2, List<String> var3, String var4) throws ItemRequirementLoadException {
      super(var2.split(var1.symbol)[0]);
      this.value = var2.split(var1.symbol)[1];
      Pattern var5 = Pattern.compile("%([^% ]+)%");
      this.staticValue = !var5.matcher(this.value).find();
      this.lore = RequirementManager.getLore(var3, Lang.PLACEHOLDER_DIFFERS_REQUIREMENT_LORE.get().toString());
      this.customMSG = var4;
   }

   @Override
   public boolean isMet(Player var1) {
      return !this.getFormatted(var1).equals(this.getValue(var1));
   }

   private String getValue(Player var1) {
      return this.staticValue ? this.value : PlaceholderAPIHook.translate(var1, this.value);
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3, "%requirement%", this.getValue(var1), "%progress%", this.getFormatted(var1));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLACEHOLDER_DIFFERS_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(var1, var2.replace("%requirement%", this.getValue(var1)).replace("%progress%", this.getFormatted(var1)));
      }
   }
}
