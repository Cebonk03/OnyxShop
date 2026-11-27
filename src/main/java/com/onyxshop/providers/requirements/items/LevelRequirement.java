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

   public LevelRequirement(String itemId, List<String> values, String text) throws ItemRequirementLoadException {
      try {
         this.requirement = Integer.parseInt(itemId);
      } catch (NumberFormatException exception) {
         throw new ItemRequirementLoadException("Invalid level for " + itemId + ", should be number");
      }

      this.lore = RequirementManager.getLore(values, Lang.LEVEL_REQUIREMENT_LORE.get().toString(), itemId);
      this.customMSG = text;
   }

   @Override
   public boolean isMet(Player player) {
      return player.getLevel() >= this.requirement;
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2, "%progress%", String.valueOf(player.getLevel()));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.LEVEL_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(player, text.replace("%requirement%", String.valueOf(this.requirement)).replace("%progress%", String.valueOf(player.getLevel())));
      }
   }
}
