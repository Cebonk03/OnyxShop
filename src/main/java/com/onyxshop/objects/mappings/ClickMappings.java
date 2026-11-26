package com.onyxshop.objects.mappings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.ClickType;

public class ClickMappings {
   private final Map<ClickType, ClickAction> mappings = new HashMap<>();

   public ClickMappings(String var1) {
      HashMap<ClickAction, ClickType> var2 = new HashMap<>();
      ClickAction[] var10000 = new ClickAction[]{ClickAction.BUY, ClickAction.SELL, ClickAction.SELL_ALL, ClickAction.GUI_EDITOR};
      ClickType[] var4 = new ClickType[]{ClickType.LEFT, ClickType.SHIFT_LEFT, ClickType.RIGHT, ClickType.SHIFT_RIGHT, ClickType.UNKNOWN};
      ConfigurationSection var5 = (var1 != null ? ConfigManager.getSection(var1) : ConfigManager.getConfig()).getConfigurationSection("click-mappings");

      for (String var7 : var5.getKeys(false)) {
         ClickAction var8 = null;
         ClickType var9 = null;

         try {
            var8 = ClickAction.valueOf(var7);
         } catch (IllegalArgumentException var14) {
            SendMessage.warnMessage(
               Lang.INVALID_CLICK_ACTION.get().replace("%click-action%", var7).replace("%click-mapping%", var7 + ": " + var5.getString(var7))
            );
            if (var1 != null) {
               SendMessage.errorSections(var1, "click-mappings");
            } else {
               SendMessage.errorItemConfig("click-mappings");
            }
         }

         try {
            String var10 = var5.getString(var7, "NONE");
            if (var10.equals("NONE")) {
               var9 = ClickType.UNKNOWN;
            } else {
               var9 = ClickType.valueOf(var5.getString(var7, "null"));
               ClickType var14 = var9;
               if (Arrays.stream(var4).noneMatch(var1x -> var1x == var14)) {
                  throw new IllegalArgumentException();
               }
            }
         } catch (IllegalArgumentException var13) {
            var9 = null;
            SendMessage.warnMessage(
               Lang.INVALID_CLICK_TYPE
                  .get()
                  .replace("%click-type%", var5.getString(var7, "null"))
                  .replace("%click-mapping%", var7 + ": " + var5.getString(var7))
            );
            if (var1 != null) {
               SendMessage.errorSections(var1, "click-mappings");
            } else {
               SendMessage.errorItemConfig("click-mappings");
            }
         }

         ConfigurationSection var18 = ConfigManager.getConfig().getDef().getConfigurationSection("click-mappings");
         if (var9 == null && var8 != null) {
            var9 = ClickType.valueOf(ConfigManager.getConfig().getDef().getString("click-mappings." + var8));
         }

         if (var8 == null && var9 != null) {
            for (String var12 : var18.getKeys(false)) {
               if (var18.getString(var12).equals(var9.name())) {
                  var8 = ClickAction.valueOf(var12);
               }
            }
         }

         var2.put(var8, var9);
      }

      if (!var2.containsKey(ClickAction.BUY)) {
         var2.put(ClickAction.BUY, Arrays.stream(var4).filter(var1x -> !var2.containsValue(var1x)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!var2.containsKey(ClickAction.SELL)) {
         var2.put(ClickAction.SELL, Arrays.stream(var4).filter(var1x -> !var2.containsValue(var1x)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!var2.containsKey(ClickAction.SELL_ALL)) {
         var2.put(ClickAction.SELL_ALL, Arrays.stream(var4).filter(var1x -> !var2.containsValue(var1x)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!var2.containsKey(ClickAction.GUI_EDITOR)) {
         var2.put(ClickAction.GUI_EDITOR, Arrays.stream(var4).filter(var1x -> !var2.containsValue(var1x)).findFirst().orElse(ClickType.UNKNOWN));
      }

      for (Entry<ClickAction, ClickType> var16 : new HashSet<>(var2.entrySet())) {
         if (var16.getValue() == ClickType.UNKNOWN) {
            var2.remove(var16.getKey());
         }
      }

      var2.forEach((var1x, var2x) -> this.mappings.put(var2x, var1x));
   }

   public ClickAction getAction(ClickType var1) {
      return this.mappings.get(var1);
   }
}
