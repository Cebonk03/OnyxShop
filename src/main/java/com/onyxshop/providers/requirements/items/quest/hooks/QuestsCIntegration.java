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

   public QuestsCIntegration(OnyxShop var1) {
      this.questsAPI = (Quests)var1.getServer().getPluginManager().getPlugin("Quests");
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
      return !this.questsAPI.isLoading();
   }

   @Override
   public String getName() {
      return "Quests by PikaMug";
   }

   @Override
   public String getQuestName(String var1) {
      for (Quest var3 : this.questsAPI.getLoadedQuests()) {
         if (var3.getId().equals(var1)) {
            return var3.getName();
         }
      }

      return null;
   }

   @Override
   public boolean isLoaded(String var1) {
      return this.questsAPI.getLoadedQuests().stream().anyMatch(var1x -> var1x.getId().equals(var1));
   }

   @Override
   public boolean hasCompleted(Player var1, String var2) {
      Quester var3 = this.questsAPI.getQuester(var1.getUniqueId());
      return var3.getCompletedQuests().stream().anyMatch(var1x -> var1x.getId().equals(var2));
   }
}
