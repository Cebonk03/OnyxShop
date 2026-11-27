package com.onyxshop.objects.inventorys;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.objects.ShopInventory;
import com.onyxshop.objects.layouts.SimpleCard;
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

public class CustomizeLayout extends ShopInventory {
   private ConversationFactory factory;
   private Conversation conv;
   private final SimpleCard card = new SimpleCard();
   private final Pattern namePattern = Pattern.compile("[a-zA-Z0-9]+");
   private final OnyxShop plugin = OnyxShop.getInstance();
   private final ArrayList<String> files = new ArrayList<>();
   private final Player player;
   private final Consumer<SimpleCard> onComplete;
   private final Prompt prompt = new StringPrompt() {
      public String getPromptText(ConversationContext conversationContext) {
         switch (CustomizeLayout.Input.valueOf((String)conversationContext.getSessionData("input"))) {
            case TITLE:
               CustomizeLayout.this.player.sendTitle("§bEnter layout title", "", 5, 80, 5);
               return "§aEnter a title for this layout.\n§aExample: ModernLayout";
            case DESC:
               CustomizeLayout.this.player.sendTitle("§bEnter description", "", 5, 80, 5);
               return "§aEnter a description for this layout.\n§aExample: Modern shop layout with almost every in-game item, designed for 1.21 survival based servers.";
            case TAGS:
               CustomizeLayout.this.player.sendTitle("§bEnter tags", "", 5, 80, 5);
               return "§aTags can be used to easily identify this layout, use the format of §f#<tag>§a, use commas(',') to specify multiple tags.\n§aExample: #modern, #allItems, #cheap";
            default:
               return null;
         }
      }

      public Prompt acceptInput(ConversationContext conversationContext, String text) {
         conversationContext.setSessionData("result", text);
         return Prompt.END_OF_CONVERSATION;
      }
   };

   public CustomizeLayout(Player player, Consumer<SimpleCard> task) {
      this.onComplete = task;
      this.player = player;
      this.startConversation(player, CustomizeLayout.Input.TITLE);
   }

   private void startConversation(Player player, CustomizeLayout.Input input) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      this.conv = this.factory
         .withFirstPrompt(this.prompt)
         .addConversationAbandonedListener(new CustomizeLayout.ConvAbandon())
         .withEscapeSequence("exit")
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(new CustomizeLayout.ConvPrefix())
         .buildConversation(player);
      this.conv.getContext().setSessionData("input", input.name());
      this.conv.begin();
   }

   private final class ConvAbandon implements ConversationAbandonedListener {
      private ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent event) {
         Player player = (Player)event.getContext().getForWhom();
         if (!event.gracefulExit()) {
            SendMessage.chatToPlayer(player, Lang.EXITED_CONVERSATION.get());
         } else {
            String text = (String)event.getContext().getSessionData("result");
            switch (CustomizeLayout.Input.valueOf((String)event.getContext().getSessionData("input"))) {
               case TITLE:
                  if (text.length() < 6) {
                     SendMessage.chatToPlayer(player, "§cTitle must be longer as 6 characters");
                     CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TITLE);
                  } else if (text.length() > 32) {
                     SendMessage.chatToPlayer(player, "§cInvalid title, title cannot be longer as 32 characters");
                     CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TITLE);
                  } else {
                     CustomizeLayout.this.card.setTitle(text);
                     CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.DESC);
                  }

                  return;
               case DESC:
                  if (text.length() < 60) {
                     SendMessage.chatToPlayer(player, "§cDescription must be longer as 60 characters, please write a descriptive explanation of your layout");
                     CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.DESC);
                     return;
                  }

                  CustomizeLayout.this.card.setDescription(text);
                  CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TAGS);
                  return;
               case TAGS:
                  ArrayList values = new ArrayList();
                  if (text.equalsIgnoreCase("skip")) {
                     SendMessage.chatToPlayer(player, "§cAtleast 1 tag is required!");
                     CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TAGS);
                     return;
                  }

                  for (String text2 : text.replace(" ", "").split(",")) {
                     if (text2.startsWith("#")) {
                        text2 = text2.substring(1);
                     }

                     if (!CustomizeLayout.this.namePattern.matcher(text2).matches()) {
                        SendMessage.chatToPlayer(player, "§cInvalid tag for " + text2 + ", contains invalid characters");
                        CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TAGS);
                        return;
                     }

                     if (text2.length() > 10) {
                        SendMessage.chatToPlayer(player, "§cInvalid tag for " + text2 + ", tag cannot be longer as 10 characters");
                        CustomizeLayout.this.startConversation(player, CustomizeLayout.Input.TAGS);
                        return;
                     }

                     values.add(text2);
                  }

                  CustomizeLayout.this.card.setTags(values);
               default:
                  CustomizeLayout.this.onComplete.accept(CustomizeLayout.this.card);
            }
         }
      }
   }

   private final class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext conversationContext) {
         return Lang.SHOP_PREFIX.get().getLegacy() + ChatColor.RESET + " ";
      }
   }

   private static enum Input {
      GUIDELINES,
      TITLE,
      DESC,
      TAGS;

      private Input() {
      }
   }
}
