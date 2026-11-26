package com.onyxshop.providers.requirements.items;

import java.util.List;
import com.onyxshop.OnyxShop;
import com.onyxshop.events.delayed.DelayedItemProvider;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.ItemManager;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.util.ItemFormat;
import com.onyxshop.util.NBTMaps;
import com.onyxshop.util.XMaterial;
import com.onyxshop.util.exceptions.ItemRequirementLoadException;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ItemRequirement implements com.onyxshop.providers.requirements.ItemRequirement, DelayedItemProvider {
   private final ItemFormat format;
   private String itemName;
   private ItemStack requirement;
   private List<String> iTags;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public ItemRequirement(String var1, List<String> var2, String var3, String var4, int var5) throws ItemRequirementLoadException {
      try {
         this.format = new ItemFormat(var1, var4, var5);
      } catch (NumberFormatException var7) {
         throw new ItemRequirementLoadException("Invalid amount for " + var1 + ", should be number");
      }

      this.lore = RequirementManager.getLore(var2, Lang.ITEM_REQUIREMENT_LORE.get().toString(), var1);
      this.customMSG = var3;
      this.load();
   }

   @Override
   public void load() throws ItemRequirementLoadException {
      if (this.format.getItem() == null) {
         String var1 = this.format.getErrorMessage();
         if (var1.contains("Item provider not enabled yet!")) {
            ItemManager.registerDelayed(this);
         } else {
            throw new ItemRequirementLoadException(var1);
         }
      } else {
         this.requirement = this.format.getItem();
         this.iTags = this.format.getiTags();

         try {
            if (XMaterial.matchXMaterial(this.requirement.getType()) == XMaterial.PLAYER_HEAD) {
               this.iTags.add(NBTMaps.getTag("SkullOwner::Id", this.format.getPath().split("\\.")[0], "." + this.format.getPath().split("\\.", 2)[1]));
            }
         } catch (IllegalArgumentException var2) {
         }

         SendMessage.infoMessage("Successfully loaded item requirement '" + this.format.getId() + "' for shop item " + this.format.getPath());
         if (!this.iTags.isEmpty()) {
            SendMessage.logDebugMessage("Ignored tags: " + String.join(", ", this.iTags));
         }

         this.itemName = this.requirement.hasItemMeta() && this.requirement.getItemMeta().hasDisplayName()
            ? this.requirement.getItemMeta().getDisplayName()
            : OnyxShop.getInstance().getMaterialName(this.requirement.getType().name());
         this.lore.replace("%itemName%", this.itemName);
         this.lore.replace("%amount%", String.valueOf(this.requirement.getAmount()));
      }
   }

   @Override
   public String getPath() {
      return this.format.getPath();
   }

   @Override
   public boolean isMet(Player var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < 36; var3++) {
         ItemStack var4 = var1.getInventory().getItem(var3);
         if (var4 != null
            && !var4.getType().equals(Material.AIR)
            && var4.getType() == this.requirement.getType()
            && OnyxShop.getInstance().isSimilar(var4, this.requirement, this.iTags)) {
            var2 += var4.getAmount();
            if (var2 >= this.requirement.getAmount()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.isReplaced() ? this.lore.get(var1, var2, var3) : this.lore.get(var1, var2, var3, "%itemName%", this.itemName);
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.ITEM_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         if (!this.lore.isReplaced()) {
            SendMessage.chatToPlayer(var1, var2);
         } else {
            SendMessage.chatToPlayer(var1, var2.replace("%itemName%", this.itemName).replace("%amount%", String.valueOf(this.requirement.getAmount())));
         }
      }
   }
}
