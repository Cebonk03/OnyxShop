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

   public QuestManager(OnyxShop plugin) {
      PluginManager pluginManager = plugin.getServer().getPluginManager();
      this.provider = this.getProvider(plugin, pluginManager);
      if (this.provider != null) {
         this.provider.init(plugin, this);
      }
   }

   private QuestProvider getProvider(OnyxShop plugin, PluginManager pluginManager) {
      if (pluginManager.getPlugin("Quests") != null) {
         try {
            Class.forName("me.pikamug.quests.Quests");
            return new QuestsCIntegration(plugin);
         } catch (ClassNotFoundException exception) {
            return new QuestsIntegration(plugin);
         }
      } else if (pluginManager.getPlugin("BeautyQuests") != null) {
         return new BeatyQuestsIntegration();
      } else {
         return pluginManager.getPlugin("BetonQuest") != null ? new BetonQuestIntegration() : null;
      }
   }

   public QuestRequirement addRequirement(QuestRequirement questRequirement, String text) {
      this.requirements.put(questRequirement, text);
      return questRequirement;
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

      for (QuestRequirement questRequirement : this.requirements.keySet()) {
         String text = this.requirements.get(questRequirement);
         if (!this.provider.isReady()) {
            SendMessage.warnMessage("Failed to load quest item requirement: Quest integration failed to load");
            SendMessage.errorShops(text.split("\\.")[0], text.split("\\.", 2)[1]);
         } else if (!this.provider.isLoaded(questRequirement.getQuestID())) {
            SendMessage.warnMessage(
               "Failed to load quest item requirement: Quest using ID '" + questRequirement.getQuestID() + "' does not exist in " + this.provider.getName()
            );
            SendMessage.errorShops(text.split("\\.")[0], text.split("\\.", 2)[1]);
         } else {
            questRequirement.onLoad();
         }
      }
   }
}
