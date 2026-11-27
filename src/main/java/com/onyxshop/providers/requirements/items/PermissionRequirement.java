package com.onyxshop.providers.requirements.items;

import java.util.List;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import org.bukkit.entity.Player;

public class PermissionRequirement implements com.onyxshop.providers.requirements.ItemRequirement {
   private final boolean meetsPerm;
   private final String requirement;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public PermissionRequirement(String permission, List<String> values, String text) {
      this.meetsPerm = !permission.startsWith("!");
      this.requirement = this.meetsPerm ? permission : permission.substring(1);
      this.lore = RequirementManager.getLore(values, Lang.PERMISSION_REQUIREMENT_LORE.get().toString(), permission);
      this.customMSG = text;
   }

   @Override
   public boolean isMet(Player player) {
      return this.meetsPerm == player.hasPermission(this.requirement);
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.get(player, flag, flag2);
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PERMISSION_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(player, text.replace("%requirement%", this.requirement));
      }
   }
}
