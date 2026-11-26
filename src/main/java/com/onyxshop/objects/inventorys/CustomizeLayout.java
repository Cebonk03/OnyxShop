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
      public String getPromptText(ConversationContext var1) {
         switch (CustomizeLayout.Input.valueOf((String)var1.getSessionData("input"))) {
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

      public Prompt acceptInput(ConversationContext var1, String var2) {
         var1.setSessionData("result", var2);
         return Prompt.END_OF_CONVERSATION;
      }
   };

   public CustomizeLayout(Player var1, Consumer<SimpleCard> var2) {
      this.onComplete = var2;
      this.player = var1;
      this.startConversation(var1, CustomizeLayout.Input.TITLE);
   }

   private void startConversation(Player var1, CustomizeLayout.Input var2) {
      this.factory = new ConversationFactory(OnyxShop.getInstance());
      this.conv = this.factory
         .withFirstPrompt(this.prompt)
         .addConversationAbandonedListener(new CustomizeLayout.ConvAbandon())
         .withEscapeSequence("exit")
         .withLocalEcho(false)
         .withTimeout(60)
         .withPrefix(new CustomizeLayout.ConvPrefix())
         .buildConversation(var1);
      this.conv.getContext().setSessionData("input", var2.name());
      this.conv.begin();
   }

   private final class ConvAbandon implements ConversationAbandonedListener {
      private ConvAbandon() {
      }

      public void conversationAbandoned(ConversationAbandonedEvent var1) {
         Player var2 = (Player)var1.getContext().getForWhom();
         if (!var1.gracefulExit()) {
            SendMessage.chatToPlayer(var2, Lang.EXITED_CONVERSATION.get());
         } else {
            String var3 = (String)var1.getContext().getSessionData("result");
            switch (CustomizeLayout.Input.valueOf((String)var1.getContext().getSessionData("input"))) {
               case TITLE:
                  if (var3.length() < 6) {
                     SendMessage.chatToPlayer(var2, "§cTitle must be longer as 6 characters");
                     CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TITLE);
                  } else if (var3.length() > 32) {
                     SendMessage.chatToPlayer(var2, "§cInvalid title, title cannot be longer as 32 characters");
                     CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TITLE);
                  } else {
                     CustomizeLayout.this.card.setTitle(var3);
                     CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.DESC);
                  }

                  return;
               case DESC:
                  if (var3.length() < 60) {
                     SendMessage.chatToPlayer(var2, "§cDescription must be longer as 60 characters, please write a descriptive explanation of your layout");
                     CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.DESC);
                     return;
                  }

                  CustomizeLayout.this.card.setDescription(var3);
                  CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TAGS);
                  return;
               case TAGS:
                  ArrayList var4 = new ArrayList();
                  if (var3.equalsIgnoreCase("skip")) {
                     SendMessage.chatToPlayer(var2, "§cAtleast 1 tag is required!");
                     CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TAGS);
                     return;
                  }

                  for (String var8 : var3.replace(" ", "").split(",")) {
                     if (var8.startsWith("#")) {
                        var8 = var8.substring(1);
                     }

                     if (!CustomizeLayout.this.namePattern.matcher(var8).matches()) {
                        SendMessage.chatToPlayer(var2, "§cInvalid tag for " + var8 + ", contains invalid characters");
                        CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TAGS);
                        return;
                     }

                     if (var8.length() > 10) {
                        SendMessage.chatToPlayer(var2, "§cInvalid tag for " + var8 + ", tag cannot be longer as 10 characters");
                        CustomizeLayout.this.startConversation(var2, CustomizeLayout.Input.TAGS);
                        return;
                     }

                     var4.add(var8);
                  }

                  CustomizeLayout.this.card.setTags(var4);
               default:
                  CustomizeLayout.this.onComplete.accept(CustomizeLayout.this.card);
            }
         }
      }
   }

   private final class ConvPrefix implements ConversationPrefix {
      private ConvPrefix() {
      }

      public String getPrefix(ConversationContext var1) {
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
