package com.onyxshop.providers.requirements.items.quest.hooks;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.quest.QuestManager;
import com.onyxshop.providers.requirements.items.quest.QuestProvider;
import me.pikamug.quests.Quests;
import me.pikamug.quests.player.Quester;
import me.pikamug.quests.quests.Quest;
import org.bukkit.entity.Player;

public class QuestsCIntegration implements QuestProvider {
   private final Quests questsAPI;

   public QuestsCIntegration(OnyxShop plugin) {
      this.questsAPI = (Quests)plugin.getServer().getPluginManager().getPlugin("Quests");
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
      return !this.questsAPI.isLoading();
   }

   @Override
   public String getName() {
      return "Quests by PikaMug";
   }

   @Override
   public String getQuestName(String name) {
      for (Quest quest : this.questsAPI.getLoadedQuests()) {
         if (quest.getId().equals(name)) {
            return quest.getName();
         }
      }

      return null;
   }

   @Override
   public boolean isLoaded(String text) {
      return this.questsAPI.getLoadedQuests().stream().anyMatch(element -> element.getId().equals(text));
   }

   @Override
   public boolean hasCompleted(Player player, String playerName) {
      Quester quester = this.questsAPI.getQuester(player.getUniqueId());
      return quester.getCompletedQuests().stream().anyMatch(element -> element.getId().equals(playerName));
   }
}
