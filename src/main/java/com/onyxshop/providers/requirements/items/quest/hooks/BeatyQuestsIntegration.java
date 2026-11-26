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
   public void init(OnyxShop var1, QuestManager var2) {
      if (!this.isReady()) {
         SendMessage.infoMessage(this.getName() + " found, waiting...");
         var1.runTaskLater(() -> {
            if (this.isReady()) {
               SendMessage.infoMessage(Lang.ENABLED_PLUGIN_HOOK.get().replace("%plugin%", this.getName()));
            } else {
               SendMessage.warnMessage(Lang.FAILED_PLUGIN_INTEGRATION.get().replace("%plugin%", this.getName()));
            }

            var2.onLoad();
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
      } catch (IllegalStateException var2) {
         return false;
      }
   }

   @Override
   public String getName() {
      return "BeautyQuests";
   }

   @Override
   public boolean isLoaded(String var1) {
      try {
         return this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(var1)) != null;
      } catch (NumberFormatException var3) {
         return false;
      }
   }

   @Override
   public String getQuestName(String var1) {
      try {
         Quest var2 = this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(var1));
         if (var2 == null) {
            throw new NullPointerException();
         } else {
            return var2.getName();
         }
      } catch (NullPointerException | NumberFormatException var3) {
         return var1;
      }
   }

   @Override
   public boolean hasCompleted(Player var1, String var2) {
      try {
         Quest var3 = this.questsAPI.getQuestsManager().getQuest(Integer.parseInt(var2));
         if (var3 == null) {
            throw new NullPointerException();
         } else {
            PlayerQuester var4 = this.questsAPI.getPlugin().getPlayersManager().getQuester(var1);
            return var4 != null && var3.hasFinished(var4);
         }
      } catch (NullPointerException | NumberFormatException var5) {
         SendMessage.warnMessage(
            "Failed to check if player " + var1.getName() + " completed quest '" + var2 + "' from BeautyQuests because it could not be loaded"
         );
         return false;
      }
   }
}
