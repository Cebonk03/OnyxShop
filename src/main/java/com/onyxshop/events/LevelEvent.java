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
   public void onPlayerLevelChange(PlayerLevelChangeEvent var1) {
      Player var2 = var1.getPlayer();
      double var3 = ConfigManager.getConfig().getDouble("leveleventmoney5/10/15");
      double var5 = ConfigManager.getConfig().getDouble("leveleventmoney20/25/30");
      Translatable var7 = Lang.LEVEL_EVENT_MESSAGE.get(var2).replace("%amount%", OnyxShop.getInstance().formatPrice((EcoType)null, var3));
      Translatable var8 = Lang.LEVEL_EVENT_MESSAGE.get(var2).replace("%amount%", OnyxShop.getInstance().formatPrice((EcoType)null, var5));
      if (ConfigManager.getConfig().getBoolean("enable-levelevent")) {
         if (var2.getLevel() == 5) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var7);
         } else if (var2.getLevel() == 10) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var7);
         } else if (var2.getLevel() == 15) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var7);
         } else if (var2.getLevel() == 20) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var8);
         } else if (var2.getLevel() == 25) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var8);
         } else if (var2.getLevel() == 30) {
            OnyxShop.getInstance().getEcoHandler().getDefaultProvider().depositBalance(var2, var3);
            SendMessage.chatToPlayer(var2, var8);
         }
      }
   }
}
