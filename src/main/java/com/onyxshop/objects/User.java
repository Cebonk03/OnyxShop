package com.onyxshop.objects;

import java.util.Locale;
import java.util.UUID;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.MenuHandler;
import com.onyxshop.providers.UserManager;
import org.bukkit.entity.Player;

public class User {
   private final UUID uuid;
   private final boolean bedrock;
   private final boolean pr;
   private Locale locale;
   private boolean openNewGUI;

   public User(Player var1) {
      this.uuid = var1.getUniqueId();
      this.pr = UserManager.isPrPlayer(var1);
      this.bedrock = UserManager.isFloodgatePlayer(var1);
      this.locale = UserManager.getLocale(var1, this.bedrock);
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public void setLocale(Locale var1) {
      this.locale = var1;
   }

   public Locale getLocale() {
      return this.locale;
   }

   public boolean isBedrock() {
      return this.bedrock;
   }

   public boolean isPr() {
      return this.pr;
   }

   public void setOpenNewGUI(boolean var1) {
      this.openNewGUI = var1;
      if (MenuHandler.backOnClose && var1) {
         OnyxShop.getInstance().runTaskLater(() -> this.setOpenNewGUI(false), 1L);
      }
   }

   public boolean isOpenNewGUI() {
      return this.openNewGUI;
   }
}
