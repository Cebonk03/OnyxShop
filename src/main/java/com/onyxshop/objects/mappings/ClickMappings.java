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

   public ClickMappings(String text) {
      HashMap<ClickAction, ClickType> map = new HashMap<>();
      ClickAction[] clickAction = new ClickAction[]{ClickAction.BUY, ClickAction.SELL, ClickAction.SELL_ALL, ClickAction.GUI_EDITOR};
      ClickType[] clickType = new ClickType[]{ClickType.LEFT, ClickType.SHIFT_LEFT, ClickType.RIGHT, ClickType.SHIFT_RIGHT, ClickType.UNKNOWN};
      ConfigurationSection section = (text != null ? ConfigManager.getSection(text) : ConfigManager.getConfig()).getConfigurationSection("click-mappings");

      for (String text2 : section.getKeys(false)) {
         ClickAction clickAction2 = null;
         ClickType clickType2 = null;

         try {
            clickAction2 = ClickAction.valueOf(text2);
         } catch (IllegalArgumentException exception) {
            SendMessage.warnMessage(
               Lang.INVALID_CLICK_ACTION.get().replace("%click-action%", text2).replace("%click-mapping%", text2 + ": " + section.getString(text2))
            );
            if (text != null) {
               SendMessage.errorSections(text, "click-mappings");
            } else {
               SendMessage.errorItemConfig("click-mappings");
            }
         }

         try {
            String text3 = section.getString(text2, "NONE");
            if (text3.equals("NONE")) {
               clickType2 = ClickType.UNKNOWN;
            } else {
               clickType2 = ClickType.valueOf(section.getString(text2, "null"));
               ClickType clickType3 = clickType2;
               if (Arrays.stream(clickType).noneMatch(element -> element == clickType3)) {
                  throw new IllegalArgumentException();
               }
            }
         } catch (IllegalArgumentException exception) {
            clickType2 = null;
            SendMessage.warnMessage(
               Lang.INVALID_CLICK_TYPE
                  .get()
                  .replace("%click-type%", section.getString(text2, "null"))
                  .replace("%click-mapping%", text2 + ": " + section.getString(text2))
            );
            if (text != null) {
               SendMessage.errorSections(text, "click-mappings");
            } else {
               SendMessage.errorItemConfig("click-mappings");
            }
         }

         ConfigurationSection section2 = ConfigManager.getConfig().getDef().getConfigurationSection("click-mappings");
         if (clickType2 == null && clickAction2 != null) {
            clickType2 = ClickType.valueOf(ConfigManager.getConfig().getDef().getString("click-mappings." + clickAction2));
         }

         if (clickAction2 == null && clickType2 != null) {
            for (String text3 : section2.getKeys(false)) {
               if (section2.getString(text3).equals(clickType2.name())) {
                  clickAction2 = ClickAction.valueOf(text3);
               }
            }
         }

         map.put(clickAction2, clickType2);
      }

      if (!map.containsKey(ClickAction.BUY)) {
         map.put(ClickAction.BUY, Arrays.stream(clickType).filter(element -> !map.containsValue(element)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!map.containsKey(ClickAction.SELL)) {
         map.put(ClickAction.SELL, Arrays.stream(clickType).filter(element -> !map.containsValue(element)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!map.containsKey(ClickAction.SELL_ALL)) {
         map.put(ClickAction.SELL_ALL, Arrays.stream(clickType).filter(element -> !map.containsValue(element)).findFirst().orElse(ClickType.UNKNOWN));
      }

      if (!map.containsKey(ClickAction.GUI_EDITOR)) {
         map.put(ClickAction.GUI_EDITOR, Arrays.stream(clickType).filter(element -> !map.containsValue(element)).findFirst().orElse(ClickType.UNKNOWN));
      }

      for (Entry<ClickAction, ClickType> entry : new HashSet<>(map.entrySet())) {
         if (entry.getValue() == ClickType.UNKNOWN) {
            map.remove(entry.getKey());
         }
      }

      map.forEach((key, value) -> this.mappings.put(value, key));
   }

   public ClickAction getAction(ClickType clickType) {
      return this.mappings.get(clickType);
   }
}
