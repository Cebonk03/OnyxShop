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

   public ItemRequirement(String itemId, List<String> values, String text, String text2, int value) throws ItemRequirementLoadException {
      try {
         this.format = new ItemFormat(itemId, text2, value);
      } catch (NumberFormatException exception) {
         throw new ItemRequirementLoadException("Invalid amount for " + itemId + ", should be number");
      }

      this.lore = RequirementManager.getLore(values, Lang.ITEM_REQUIREMENT_LORE.get().toString(), itemId);
      this.customMSG = text;
      this.load();
   }

   @Override
   public void load() throws ItemRequirementLoadException {
      if (this.format.getItem() == null) {
         String errorMessage = this.format.getErrorMessage();
         if (errorMessage.contains("Item provider not enabled yet!")) {
            ItemManager.registerDelayed(this);
         } else {
            throw new ItemRequirementLoadException(errorMessage);
         }
      } else {
         this.requirement = this.format.getItem();
         this.iTags = this.format.getiTags();

         try {
            if (XMaterial.matchXMaterial(this.requirement.getType()) == XMaterial.PLAYER_HEAD) {
               this.iTags.add(NBTMaps.getTag("SkullOwner::Id", this.format.getPath().split("\\.")[0], "." + this.format.getPath().split("\\.", 2)[1]));
            }
         } catch (IllegalArgumentException exception) {
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
   public boolean isMet(Player player) {
      int value = 0;

      for (int index = 0; index < 36; index++) {
         ItemStack itemStack = player.getInventory().getItem(index);
         if (itemStack != null
            && !itemStack.getType().equals(Material.AIR)
            && itemStack.getType() == this.requirement.getType()
            && OnyxShop.getInstance().isSimilar(itemStack, this.requirement, this.iTags)) {
            value += itemStack.getAmount();
            if (value >= this.requirement.getAmount()) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.isReplaced() ? this.lore.get(player, flag, flag2) : this.lore.get(player, flag, flag2, "%itemName%", this.itemName);
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.ITEM_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         if (!this.lore.isReplaced()) {
            SendMessage.chatToPlayer(player, text);
         } else {
            SendMessage.chatToPlayer(player, text.replace("%itemName%", this.itemName).replace("%amount%", String.valueOf(this.requirement.getAmount())));
         }
      }
   }
}
