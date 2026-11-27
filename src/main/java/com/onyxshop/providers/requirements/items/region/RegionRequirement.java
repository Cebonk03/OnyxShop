package com.onyxshop.providers.requirements.items.region;

import java.util.List;
import com.onyxshop.files.Lang;
import com.onyxshop.files.Translatable;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.RequirementManager;
import com.onyxshop.providers.requirements.ItemRequirement;
import org.bukkit.entity.Player;

public class RegionRequirement implements ItemRequirement {
   private final String regionID;
   private final RegionProvider regionProvider;
   private final String customMSG;
   private final RequirementManager.RequirementLore lore;

   public RegionRequirement(RegionProvider regionProvider, String text, List<String> values, String text2) {
      this.regionID = text;
      this.regionProvider = regionProvider;
      this.lore = RequirementManager.getLore(values, Lang.REGION_REQUIREMENT_LORE.get().toString());
      this.customMSG = text2;
      if (this.regionProvider.isReady()) {
         this.onLoad();
      }
   }

   public String getRegionID() {
      return this.regionID;
   }

   public void onLoad() {
      if (!this.regionProvider.isLoaded(this.regionID)) {
         SendMessage.logDebugMessage(
            "Region using ID '"
               + this.regionID
               + "' wasn't found yet after provider enabled, is this supposed to be a region which isn't created yet? Else the name of the region is incorrect for the item requirement"
         );
      } else {
         this.lore.replace("%regionName%", this.regionProvider.getRegionName(this.regionID));
      }
   }

   @Override
   public boolean isMet(Player player) {
      return this.regionProvider.isInsideRegion(player, this.regionID);
   }

   @Override
   public String[] getLore(Player player, boolean flag, boolean flag2) {
      return this.lore.isReplaced()
         ? this.lore.get(player, flag, flag2)
         : this.lore.get(player, flag, flag2, "%regionName%", this.regionProvider.getRegionName(this.regionID));
   }

   @Override
   public void sendNotMetMessage(Player player) {
      Translatable text = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.REGION_REQUIREMENT_NOT_MET.get(player);
      if (!text.isEmpty()) {
         SendMessage.chatToPlayer(player, text.replace("%regionName%", this.regionProvider.getRegionName(this.regionID)));
      }
   }
}
