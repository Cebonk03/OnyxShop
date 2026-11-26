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

   public PlaytimeRequirement(int var1, String var2, List<String> var3, String var4) throws ItemRequirementLoadException {
      this.stat = Statistic.valueOf(var1 > 112 ? "PLAY_ONE_MINUTE" : "PLAY_ONE_TICK");

      try {
         this.requirement = (int)TimeUtils.getTime(var2) / 50;
      } catch (ParseException var6) {
         throw new ItemRequirementLoadException("Invalid play time requirement found: " + var6.getMessage());
      }

      this.lore = RequirementManager.getLore(var3, Lang.PLAYTIME_REQUIREMENT_NOT_MET.get().toString(), var2);
      this.customMSG = var4;
   }

   @Override
   public boolean isMet(Player var1) {
      return var1.getStatistic(this.stat) >= this.requirement;
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3, "%progress%", TimeUtils.getReadableTime(var1.getStatistic(this.stat) * 50L));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PLAYTIME_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(
            var1,
            var2.replace("%requirement%", TimeUtils.getReadableTime(this.requirement * 50L))
               .replace("%progress%", TimeUtils.getReadableTime(var1.getStatistic(this.stat) * 50L))
         );
      }
   }
}
