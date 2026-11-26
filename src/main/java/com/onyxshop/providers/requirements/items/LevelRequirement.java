package com.onyxshop.providers.requirements.items;

import java.util.List;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class LevelRequirement implements com.onyxshop.providers.requirements.ItemRequirement {
   private final int requirement;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public LevelRequirement(String var1, List<String> var2, String var3) throws ItemRequirementLoadException {
      try {
         this.requirement = Integer.parseInt(var1);
      } catch (NumberFormatException var5) {
         throw new ItemRequirementLoadException("Invalid level for " + var1 + ", should be number");
      }

      this.lore = RequirementManager.getLore(var2, Lang.LEVEL_REQUIREMENT_LORE.get().toString(), var1);
      this.customMSG = var3;
   }

   @Override
   public boolean isMet(Player var1) {
      return var1.getLevel() >= this.requirement;
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3, "%progress%", String.valueOf(var1.getLevel()));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.LEVEL_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(var1, var2.replace("%requirement%", String.valueOf(this.requirement)).replace("%progress%", String.valueOf(var1.getLevel())));
      }
   }
}
