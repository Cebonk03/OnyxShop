package com.onyxshop.providers.requirements.items.quest;

import java.util.List;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.entity.Player;

public class QuestRequirement implements ItemRequirement {
   private final String questID;
   private final QuestProvider questProvider;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public QuestRequirement(QuestProvider var1, String var2, List<String> var3, String var4) throws ItemRequirementLoadException {
      this.questID = var2;
      this.questProvider = var1;
      this.lore = RequirementManager.getLore(var3, Lang.QUEST_REQUIREMENT_LORE.get().toString());
      this.customMSG = var4;
      if (this.questProvider.isReady()) {
         if (!this.questProvider.isLoaded(var2)) {
            throw new ItemRequirementLoadException("Quest using ID '" + var2 + "' isn't found");
         }

         this.onLoad();
      }
   }

   public String getQuestID() {
      return this.questID;
   }

   public void onLoad() {
      this.lore.replace("%questName%", this.questProvider.getQuestName(this.questID));
   }

   @Override
   public boolean isMet(Player var1) {
      return this.questProvider.hasCompleted(var1, this.questID);
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.isReplaced()
         ? this.lore.get(var1, var2, var3)
         : this.lore.get(var1, var2, var3, "%questName%", this.questProvider.getQuestName(this.questID));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.QUEST_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(var1, var2.replace("%questName%", this.questProvider.getQuestName(this.questID)));
      }
   }
}
