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
      BetonQuestApiService var1 = (BetonQuestApiService)Bukkit.getServer().getServicesManager().load(BetonQuestApiService.class);
      if (var1 != null) {
         this.questsAPI = var1.api(OnyxShop.getInstance());
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
   public boolean isLoaded(String var1) {
      return true;
   }

   @Override
   public String getQuestName(String var1) {
      return var1;
   }

   @Override
   public boolean hasCompleted(Player var1, String var2) {
      OnlineProfile var3 = this.questsAPI.profiles().getProfile(var1);
      return this.questsAPI.persistence().of(var3).tags().has(var2);
   }
}
