package com.onyxshop.providers.requirements.items.quest;

import java.util.HashMap;
import java.util.Map;
import com.onyxshop.OnyxShop;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.requirements.items.quest.hooks.BeatyQuestsIntegration;
import com.onyxshop.providers.requirements.items.quest.hooks.BetonQuestIntegration;
import com.onyxshop.providers.requirements.items.quest.hooks.QuestsCIntegration;
import com.onyxshop.providers.requirements.items.quest.hooks.QuestsIntegration;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.plugin.PluginManager;

public class QuestManager {
   private final QuestProvider provider;
   private final Map<QuestRequirement, String> requirements = new HashMap<>();

   public QuestManager(OnyxShop var1) {
      PluginManager var2 = var1.getServer().getPluginManager();
      this.provider = this.getProvider(var1, var2);
      if (this.provider != null) {
         this.provider.init(var1, this);
      }
   }

   private QuestProvider getProvider(OnyxShop var1, PluginManager var2) {
      if (var2.getPlugin("Quests") != null) {
         try {
            Class.forName("me.pikamug.quests.Quests");
            return new QuestsCIntegration(var1);
         } catch (ClassNotFoundException var4) {
            return new QuestsIntegration(var1);
         }
      } else if (var2.getPlugin("BeautyQuests") != null) {
         return new BeatyQuestsIntegration();
      } else {
         return var2.getPlugin("BetonQuest") != null ? new BetonQuestIntegration() : null;
      }
   }

   public QuestRequirement addRequirement(QuestRequirement var1, String var2) {
      this.requirements.put(var1, var2);
      return var1;
   }

   public QuestProvider getProvider() throws ItemRequirementLoadException {
      if (this.provider != null) {
         return this.provider;
      } else {
         throw new ItemRequirementLoadException("Failed to enable integration with supported Quest plugin");
      }
   }

   public void onLoad() {
      if (!this.provider.isReady()) {
         SendMessage.errorMessage("Failed to enable quest integration with '" + this.provider.getName() + "'");
      }

      for (QuestRequirement var2 : this.requirements.keySet()) {
         String var3 = this.requirements.get(var2);
         if (!this.provider.isReady()) {
            SendMessage.warnMessage("Failed to load quest item requirement: Quest integration failed to load");
            SendMessage.errorShops(var3.split("\\.")[0], var3.split("\\.", 2)[1]);
         } else if (!this.provider.isLoaded(var2.getQuestID())) {
            SendMessage.warnMessage(
               "Failed to load quest item requirement: Quest using ID '" + var2.getQuestID() + "' does not exist in " + this.provider.getName()
            );
            SendMessage.errorShops(var3.split("\\.")[0], var3.split("\\.", 2)[1]);
         } else {
            var2.onLoad();
         }
      }
   }
}
