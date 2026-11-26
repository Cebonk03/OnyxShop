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

   public RegionRequirement(RegionProvider var1, String var2, List<String> var3, String var4) {
      this.regionID = var2;
      this.regionProvider = var1;
      this.lore = RequirementManager.getLore(var3, Lang.REGION_REQUIREMENT_LORE.get().toString());
      this.customMSG = var4;
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
   public boolean isMet(Player var1) {
      return this.regionProvider.isInsideRegion(var1, this.regionID);
   }

   @Override
   public String[] getLore(Player var1, boolean var2, boolean var3) {
      return this.lore.isReplaced()
         ? this.lore.get(var1, var2, var3)
         : this.lore.get(var1, var2, var3, "%regionName%", this.regionProvider.getRegionName(this.regionID));
   }

   @Override
   public void sendNotMetMessage(Player var1) {
      Translatable var2 = this.customMSG != null ? SendMessage.fromConfig(this.customMSG) : Lang.REGION_REQUIREMENT_NOT_MET.get(var1);
      if (!var2.isEmpty()) {
         SendMessage.chatToPlayer(var1, var2.replace("%regionName%", this.regionProvider.getRegionName(this.regionID)));
      }
   }
}
