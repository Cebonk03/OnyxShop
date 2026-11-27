package com.onyxshop.providers.requirements.items.quest.hooks;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.quest.QuestManager;
import com.onyxshop.providers.requirements.items.quest.QuestProvider;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.BetonQuestApiService;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BetonQuestIntegration implements QuestProvider {
   private BetonQuestApi questsAPI;

   public BetonQuestIntegration() {
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
      BetonQuestApiService betonQuestApiService = (BetonQuestApiService)Bukkit.getServer().getServicesManager().load(BetonQuestApiService.class);
      if (betonQuestApiService != null) {
         this.questsAPI = betonQuestApiService.api(OnyxShop.getInstance());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String getName() {
      return "BetonQuest";
   }

   @Override
   public boolean isLoaded(String text) {
      return true;
   }

   @Override
   public String getQuestName(String name) {
      return name;
   }

   @Override
   public boolean hasCompleted(Player player, String questId) {
      OnlineProfile onlineProfile = this.questsAPI.profiles().getProfile(player);
      return this.questsAPI.persistence().of(onlineProfile).tags().has(questId);
   }
}
