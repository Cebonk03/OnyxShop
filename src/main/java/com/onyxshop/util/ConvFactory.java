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
      public String getPromptText(ConversationContext var1) {
         return Lang.NAME_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData(var1.getSessionData("option"), var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt lore = new StringPrompt() {
      public String getPromptText(ConversationContext var1) {
         return Lang.LORE_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData(var1.getSessionData("option"), var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt armorcolor = new StringPrompt() {
      public String getPromptText(ConversationContext var1) {
         return null;
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         return null;
      }
   };
   Prompt commands = new StringPrompt() {
      public String getPromptText(ConversationContext var1) {
         return Lang.COMMAND_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData(var1.getSessionData("option"), var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt nbtData = new StringPrompt() {
      public String getPromptText(ConversationContext var1) {
         return Lang.NBTDATA_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData(var1.getSessionData("option"), var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };
   Prompt frequency = new StringPrompt() {
      public String getPromptText(ConversationContext var1) {
         return Lang.RESTOCK_FREQUENCY_PROMPT_TEXT.get().getLegacy();
      }

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData(var1.getSessionData("option"), var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };

   public ConvFactory() {
   }

   public void startConversation(Player var1, ItemEditor.Option var2, ItemEditor.Action var3, String var4, int var5) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      switch (var2) {
         case DISPLAYNAME:
         case NAME:
            this.conv = this.factory
               .withFirstPrompt(this.name)
               .addConversationAbandonedListener(new ConvFactory.ConvAbandon())
               .withEscapeSequence("exit")
               .withLocalEcho(false)
               .withTimeout(60)
               .withPrefix(new ConvFactory.ConvPrefix())
               .buildConversation(var1);
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
               .buildConversation(var1);
            this.conv.getContext().setSessionData("index", var5);
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
               .buildConversation(var1);
            this.conv.getContext().setSessionData("index", var5);
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
               .buildConversation(var1);
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
               .buildConversation(var1);
      }

      this.conv.getContext().setSessionData("option", var2);
      this.conv.getContext().setSessionData("action", var3);
      this.conv.getContext().setSessionData("itemPath", var4);
      this.conv.begin();
   }

   class ConvAbandon implements ConversationAbandonedListener {
      ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent var1) {
         Player var2 = (Player)var1.getContext().getForWhom();
         if (var1.gracefulExit()) {
            ItemEditor.Option var3 = (ItemEditor.Option)var1.getContext().getSessionData("option");
            ItemEditor.Action var4 = (ItemEditor.Action)var1.getContext().getSessionData("action");
            String[] var5 = ((String)var1.getContext().getSessionData("itemPath")).split("\\.");
            String var6 = var5[0];
            String var7 = var5[1] + "." + var5[2] + "." + var5[3];
            switch (var3) {
               case DISPLAYNAME:
               case NAME:
                  ConfigManager.getShop(var6).set("pages." + var7 + "." + var3.getKey(), var1.getContext().getSessionData(var3));
                  break;
               case LORE:
               case DISPLAYLORE:
               case BUY_COMMANDS:
               case SELL_COMMANDS:
                  List var8 = ConfigManager.getShop(var6).getStringList("pages." + var7 + "." + var3.getKey());
                  String var9 = String.valueOf(var1.getContext().getSessionData(var3));
                  if (var4 == ItemEditor.Action.ADD) {
                     var8.add(var9);
                  } else if (var4 == ItemEditor.Action.EDIT) {
                     Integer var15 = (Integer)var1.getContext().getSessionData("index");
                     if (var15 != null && var15 != -1) {
                        var8.set(var15, var9);
                     } else {
                        var8.clear();
                        var8.add(var9);
                     }
                  }

                  ConfigManager.getShop(var6).set("pages." + var7 + "." + var3.getKey(), var8);
                  break;
               case NBTDATA:
               case COMPONENTS:
                  ConfigManager.getShop(var6).set("pages." + var7 + "." + var3.getKey(), var1.getContext().getSessionData(var3));
                  break;
               case GLOBAL_STOCK_FREQUENCY:
               case PLAYER_STOCK_FREQUENCY:
                  String var10 = (String)var1.getContext().getSessionData(var3);

                  try {
                     LinkedList var11 = OnyxShop.getInstance().getScheduleManager().validate(var10);
                     if (var11.isEmpty()) {
                        SendMessage.warnMessage(var2, "Invalid cron expression, this schedule will not fire again.");
                        ConvFactory.this.startConversation(var2, var3, var4, var6 + "." + var7, -1);
                        return;
                     }

                     StringBuilder var12 = new StringBuilder();

                     for (int var13 = 0; var13 < var11.size(); var13++) {
                        var12.append(
                           ChatColor.GRAY
                              + "-"
                              + ChatColor.GREEN
                              + " "
                              + TimeUtils.getDateTime(((Date)var11.get(var13)).getTime())
                              + (var13 != var11.size() ? "\n" : "")
                        );
                     }

                     SendMessage.infoMessage(var2, ChatColor.GREEN + "Loaded cron job with following schedule dates loaded(up to 5):\n" + var12.toString());
                     ConfigManager.getShop(var6).set("pages." + var7 + "." + var3.getKey(), var10);
                  } catch (RuntimeException var14) {
                     SendMessage.warnMessage(
                        var2, ChatColor.RED + "Invalid cron expression format with reason(See console for more details): " + var14.getMessage()
                     );
                     SendMessage.warnMessage(ChatColor.RED + "Invalid cron expression format specified while executing command");
                     var14.printStackTrace();
                     ConvFactory.this.startConversation(var2, var3, var4, var6 + "." + var7, -1);
                     return;
                  }
            }

            ConfigManager.saveShop(var6);
            new ConclusionScreen(var2, OnyxShop.getInstance().getShopItem(var6, var7), var3, var4, null);
         } else {
            SendMessage.chatToPlayer(var2, Lang.EXITED_CONVERSATION.get());
         }
      }
   }

   private static class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext var1) {
         return Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ";
      }
   }
}
