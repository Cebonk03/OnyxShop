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

   public QuestRequirement(QuestProvider questProvider, String itemId, List<String> values, String text) throws ItemRequirementLoadException {
      this.questID = itemId;
      this.questProvider = questProvider;
      this.lore = RequirementManager.getLore(values, Lang.QUEST_REQUIREMENT_LORE.get().toString());
      this.customMSG = text;
      if (this.questProvider.isReady()) {
         if (!this.questProvider.isLoaded(itemId)) {
            throw new ItemRequirementLoadException("Quest using ID '" + itemId + "' isn't found");
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
   public boolean isMet(Player player) {
      return this.questProvider.hasCompleted(player, this.questID);
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.isReplaced()
         ? this.lore.get(player, flag, flag2)
         : this.lore.get(player, flag, flag2, "%questName%", this.questProvider.getQuestName(this.questID));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.QUEST_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(player, text.replace("%questName%", this.questProvider.getQuestName(this.questID)));
      }
   }
}
