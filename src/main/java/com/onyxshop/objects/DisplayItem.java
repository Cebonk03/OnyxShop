package com.onyxshop.objects;

import java.util.Collection;
import java.util.Map;
import com.onyxshop.providers.requirements.ItemRequirement;
import com.onyxshop.providers.requirements.RequirementType;
import com.onyxshop.providers.requirements.Requirements;
import com.onyxshop.util.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface DisplayItem {
   String section();

   String itemLoc();

   String getItemPath();

   String getDisplayname();

   ItemStack getShopItem();

   boolean hasPlaceholders();

   Map<Integer, Placeholder> getPlaceholders();

   Requirements getRequirements();

   boolean meetsRequirements(Player var1, boolean var2);

   boolean hasItemRequirement(RequirementType var1);

   void addItemRequirement(ItemRequirement var1);

   void removeItemRequirement(ItemRequirement var1);

   void addItemRequirements(Collection<? extends ItemRequirement> var1);

   void removeItemRequirements(Collection<? extends ItemRequirement> var1);

   boolean hasItemError();

   boolean rotates();

   boolean isCloseMenu();

   boolean isDisplayItem();

   boolean isClickCommand();

   boolean canPurchase(Player var1, String var2, boolean var3);

   void updateSkullTexture(Object var1, boolean var2);

   DisplayItem.DynamicLore getLore();

   public interface DynamicLore {
      String[] get(boolean var1, boolean var2);
   }
}
