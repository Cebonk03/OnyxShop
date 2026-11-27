package com.onyxshop.util;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.inventorys.editor.items.ConclusionScreen;
import com.onyxshop.methodes.ItemEditor;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.ChatColor;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;

public class ConvFactory {
   private ConversationFactory factory;
   private Conversation conv;
   Prompt name = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return Lang.NAME_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData(conversationContext.getSessionData("option"), text);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt lore = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return Lang.LORE_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData(conversationContext.getSessionData("option"), text);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt armorcolor = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return null;
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         return null;
      }
   };
   Prompt commands = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return Lang.COMMAND_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData(conversationContext.getSessionData("option"), text);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt nbtData = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return Lang.NBTDATA_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData(conversationContext.getSessionData("option"), text);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt frequency = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         return Lang.RESTOCK_FREQUENCY_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData(conversationContext.getSessionData("option"), text);
         return Prompt.END_OF_CONVERSATION;
      }
   };

   public ConvFactory() {
   }

   public void startConversation(Player player, ItemEditor.Option option, ItemEditor.Action action, String playerName, int value) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      switch (option) {
         case DISPLAYNAME:
         case NAME:
            this.conv = this.factory
               .withFirstPrompt(this.name)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(player);
            break;
         case LORE:
         case DISPLAYLORE:
            this.conv = this.factory
               .withFirstPrompt(this.lore)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(player);
            this.conv.getContext().setSessionData("index", value);
            break;
         case BUY_COMMANDS:
         case SELL_COMMANDS:
            this.conv = this.factory
               .withFirstPrompt(this.commands)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(player);
            this.conv.getContext().setSessionData("index", value);
            break;
         case NBTDATA:
         case COMPONENTS:
            this.conv = this.factory
               .withFirstPrompt(this.nbtData)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(player);
            break;
         case GLOBAL_STOCK_FREQUENCY:
         case PLAYER_STOCK_FREQUENCY:
            this.conv = this.factory
               .withFirstPrompt(this.frequency)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(player);
      }

      this.conv.getContext().setSessionData("option", option);
      this.conv.getContext().setSessionData("action", action);
      this.conv.getContext().setSessionData("itemPath", playerName);
      this.conv.begin();
   }

   class ConvAbandon implements ConversationAbandonedListener {
      ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent event) {
         Player player = (Player)event.getContext().getForWhom();
         if (event.gracefulExit()) {
            ItemEditor.Option option = (ItemEditor.Option)event.getContext().getSessionData("option");
            ItemEditor.Action action = (ItemEditor.Action)event.getContext().getSessionData("action");
            String[] itemIds = ((String)event.getContext().getSessionData("itemPath")).split("\\.");
            String text = itemIds[0];
            String text2 = itemIds[1] + "." + itemIds[2] + "." + itemIds[3];
            switch (option) {
               case DISPLAYNAME:
               case NAME:
                  ConfigManager.getShop(text).set("pages." + text2 + "." + option.getKey(), event.getContext().getSessionData(option));
                  break;
               case LORE:
               case DISPLAYLORE:
               case BUY_COMMANDS:
               case SELL_COMMANDS:
                  List stringList = ConfigManager.getShop(text).getStringList("pages." + text2 + "." + option.getKey());
                  String text3 = String.valueOf(event.getContext().getSessionData(option));
                  if (action == ItemEditor.Action.ADD) {
                     stringList.add(text3);
                  } else if (action == ItemEditor.Action.EDIT) {
                     Integer value = (Integer)event.getContext().getSessionData("index");
                     if (value != null && value != -1) {
                        stringList.set(value, text3);
                     } else {
                        stringList.clear();
                        stringList.add(text3);
                     }
                  }

                  ConfigManager.getShop(text).set("pages." + text2 + "." + option.getKey(), stringList);
                  break;
               case NBTDATA:
               case COMPONENTS:
                  ConfigManager.getShop(text).set("pages." + text2 + "." + option.getKey(), event.getContext().getSessionData(option));
                  break;
               case GLOBAL_STOCK_FREQUENCY:
               case PLAYER_STOCK_FREQUENCY:
                  String text4 = (String)event.getContext().getSessionData(option);

                  try {
                     LinkedList shopIds = OnyxShop.getInstance().getScheduleManager().validate(text4);
                     if (shopIds.isEmpty()) {
                        SendMessage.warnMessage(player, "Invalid cron expression, this schedule will not fire again.");
                        ConvFactory.this.startConversation(player, option, action, text + "." + text2, -1);
                        return;
                     }

                     StringBuilder builder = new StringBuilder();

                     for (int size = 0; size < shopIds.size(); size++) {
                        builder.append(
                           ChatColor.GRAY
                              + "-"
                              + ChatColor.GREEN
                              + " "
                              + TimeUtils.getDateTime(((Date)shopIds.get(size)).getTime())
                              + (size != shopIds.size() ? "\n" : "")
                        );
                     }

                     SendMessage.infoMessage(player, ChatColor.GREEN + "Loaded cron job with following schedule dates loaded(up to 5):\n" + builder.toString());
                     ConfigManager.getShop(text).set("pages." + text2 + "." + option.getKey(), text4);
                  } catch (RuntimeException exception) {
                     SendMessage.warnMessage(
                        player, ChatColor.RED + "Invalid cron expression format with reason(See console for more details): " + exception.getMessage()
                     );
                     SendMessage.warnMessage(ChatColor.RED + "Invalid cron expression format specified while executing command");
                     exception.printStackTrace();
                     ConvFactory.this.startConversation(player, option, action, text + "." + text2, -1);
                     return;
                  }
            }

            ConfigManager.saveShop(text);
            new ConclusionScreen(player, OnyxShop.getInstance().getShopItem(text, text2), option, action, null);
         } else {
            SendMessage.chatToPlayer(player, Lang.EXITED_CONVERSATION.get());
         }
      }
   }

   private static class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext conversationContext) {
         return Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ";
      }
   }
}
