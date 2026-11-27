package com.onyxshop.events;

import com.onyxshop.OnyxShop;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.EcoType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLevelChangeEvent;

public class LevelEvent implements Listener {
   public LevelEvent() {
   }

   @EventHandler
   public void onPlayerLevelChange(PlayerLevelChangeEvent event) {
      Player player = event.getPlayer();
      double amount = ConfigManager.getConfig().getDouble("leveleventmoney5/10/15");
      double amount2 = ConfigManager.getConfig().getDouble("leveleventmoney20/25/30");
      Translatable text = Lang.LEVEL_EVENT_MESSAGE.get(player).replace("%amount%", OnyxShop.getInstance().formatPrice((EcoType)null, amount));
      Translatable text2 = Lang.LEVEL_EVENT_MESSAGE.get(player).replace("%amount%", OnyxShop.getInstance().formatPrice((EcoType)null, amount2));
      if (ConfigManager.getConfig().getBoolean("enable-levelevent")) {
         if (player.getLevel() == 5) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text);
         } else if (player.getLevel() == 10) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text);
         } else if (player.getLevel() == 15) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text);
         } else if (player.getLevel() == 20) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text2);
         } else if (player.getLevel() == 25) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text2);
         } else if (player.getLevel() == 30) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(player, amount);
            SendMessage.chatToPlayer(player, text2);
         }
      }
   }
}
