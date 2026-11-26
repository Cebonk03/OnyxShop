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

   public PermissionRequirement(String var1, List<String> var2, String var3) {
      this.meetsPerm = !var1.startsWith("!");
      this.requirement = this.meetsPerm ? var1 : var1.substring(1);
      this.lore = RequirementManager.getLore(var2, Lang.PERMISSION_REQUIREMENT_LORE.get().toString(), var1);
      this.customMSG = var3;
   }

   @Override
   public boolean isMet(Player var1) {
      return this.meetsPerm == var1.hasPermission(this.requirement);
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.get(var1, var2, var3);
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.PERMISSION_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(var1, var2.replace("%requirement%", this.requirement));
      }
   }
}
