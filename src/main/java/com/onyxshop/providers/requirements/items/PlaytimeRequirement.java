package com.onyxshop.providers.requirements.items;

import java.text.ParseException;
import java.util.List;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.util.TimeUtils;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlaytimeRequirement implements com.onyxshop.providers.requirements.ItemRequirement {
   private final Statistic stat;
   private final int requirement;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public PlaytimeRequirement(int value, String itemId, List<String> values, String text) throws ItemRequirementLoadException {
      this.stat = Statistic.valueOf(value > 112 ? "PLAY_ONE_MINUTE" : "PLAY_ONE_TICK");

      try {
         this.requirement = (int)TimeUtils.getTime(itemId) / 50;
      } catch (ParseException exception) {
         throw new ItemRequirementLoadException("Invalid play time requirement found: " + exception.getMessage());
      }

      this.lore = RequirementManager.getLore(values, Lang.PLAYTIME_REQUIREMENT_NOT_MET.get().toString(), itemId);
      this.customMSG = text;
   }

   @Override
   public boolean isMet(Player player) {
      return player.getStatistic(this.stat) >= this.requirement;
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2, "%progress%", TimeUtils.getReadableTime(player.getStatistic(this.stat) * 50L));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLAYTIME_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(
            player,
            text.replace("%requirement%", TimeUtils.getReadableTime(this.requirement * 50L))
               .replace("%progress%", TimeUtils.getReadableTime(player.getStatistic(this.stat) * 50L))
         );
      }
   }
}
