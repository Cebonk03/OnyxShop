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

   boolean meetsRequirements(Player player, boolean flag);

   boolean hasItemRequirement(RequirementType requirementType);

   void addItemRequirement(ItemRequirement itemRequirement);

   void removeItemRequirement(ItemRequirement itemRequirement);

   void addItemRequirements(Collection<? extends ItemRequirement> values);

   void removeItemRequirements(Collection<? extends ItemRequirement> values);

   boolean hasItemError();

   boolean rotates();

   boolean isCloseMenu();

   boolean isDisplayItem();

   boolean isClickCommand();

   boolean canPurchase(Player player, String playerName, boolean flag);

   void updateSkullTexture(Object value, boolean flag);

   DisplayItem.DynamicLore getLore();

   public interface DynamicLore {
      String[] get(boolean flag, boolean flag2);
   }
}
