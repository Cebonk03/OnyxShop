package com.onyxshop.providers.requirements.items.quest.hooks;

import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.players.PlayerQuester;
import fr.skytasul.quests.api.quests.Quest;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.quest.QuestManager;
import com.onyxshop.providers.requirements.items.quest.QuestProvider;
import org.bukkit.entity.Player;

public class BeatyQuestsIntegration implements QuestProvider {
   private QuestsAPI questsAPI;

   public BeatyQuestsIntegration() {
   }

   @Override
   public void init(OnyxShop plugin, QuestManager questManager) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         plugin.runTaskLater(() -> {
            if (this.isReady()) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            questManager.onLoad();
         }, 65L);
      } else {
         SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
      }
   }

   @Override
   public boolean isReady() {
      try {
         this.questsAPI = QuestsAPI.getAPI();
         return true;
      } catch (IllegalStateException exception) {
         return false;
      }
   }

   @Override
   public String getName() {
      return "BeautyQuests";
   }

   @Override
   public boolean isLoaded(String text) {
      try {
         return this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(text)) != null;
      } catch (NumberFormatException exception) {
         return false;
      }
   }

   @Override
   public String getQuestName(String name) {
      try {
         Quest quest = this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(name));
         if (quest == null) {
            throw new NullPointerException();
         } else {
            return quest.getName();
         }
      } catch (NullPointerException | NumberFormatException exception) {
         return name;
      }
   }

   @Override
   public boolean hasCompleted(Player player, String playerName) {
      try {
         Quest quest = this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(playerName));
         if (quest == null) {
            throw new NullPointerException();
         } else {
            PlayerQuester playerQuester = this.questsAPI.getPlugin().getPlayersManager().getQuester(player);
            return playerQuester != null && quest.hasFinished(playerQuester);
         }
      } catch (NullPointerException | NumberFormatException exception) {
         SendMessage.warnMessage(
            "Failed to check if player " + player.getName() + " completed quest '" + playerName + "' from BeautyQuests because it could not be loaded"
         );
         return false;
      }
   }
}
