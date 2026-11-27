package com.onyxshop.commands.editshop.subcommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import com.onyxshop.OnyxShop;
import com.onyxshop.commands.editshop.Methods;
import com.onyxshop.commands.editshop.SubCommad;
import com.onyxshop.files.ConfigManager;
import com.onyxshop.files.Lang;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.util.PermissionsCache;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.StringUtil;

public class AddSection extends SubCommad {
   private final Methods methods;
   private final OnyxShop plugin;
   private final Pattern p = Pattern.compile("[^a-zA-Z0-9]");
   private String section;
   private String material;
   private String displayname;
   private Integer slot;

   public AddSection(OnyxShop plugin, Methods methods) {
      this.plugin = plugin;
      this.methods = methods;
   }

   @Override
   public String getName() {
      return "addsection";
   }

   @Override
   public String getDescription() {
      return Lang.EDITSHOP_ADD_SECTION_SUBCOMMAND_DESC.get().getLegacy();
   }

   @Override
   public String getSyntax() {
      return Lang.EDITSHOP_ADD_SECTION_SUBCOMMAND_SYNTAX.get().getLegacy();
   }

   @Override
   public boolean hasPermission(CommandSender sender) {
      return PermissionsCache.hasPermission(sender, "OnyxShop.editshop." + this.getName());
   }

   @Override
   public void perform(Object value, String[] values) {
      if (values.length > 1) {
         if (!this.p.matcher(values[1]).find()) {
            this.section = values[1];
            if (values.length > 2) {
               this.material = this.methods.getMaterial(value, values[2]);
               if (this.material != null) {
                  if (values.length > 3) {
                     this.displayname = values[3];
                     if (values.length > 4) {
                        this.slot = this.methods.getMainMenuSlot(value, values[4]);
                        if (this.slot != null) {
                           this.addSectionToSectionsConfig(value);
                        }
                     } else {
                        SendMessage.sendMessage(value, this.getSyntax());
                     }
                  } else {
                     SendMessage.sendMessage(value, this.getSyntax());
                  }
               }
            } else {
               SendMessage.sendMessage(value, this.getSyntax());
            }
         } else {
            SendMessage.sendMessage(value, ChatColor.RED + "The section name can only contain letters and numbers.");
         }
      } else {
         SendMessage.sendMessage(value, this.getSyntax());
      }
   }

   @Override
   public List<String> getTabCompletion(String[] values) {
      switch (values.length) {
         case 2:
            return Arrays.asList("Wood", "Armor", "Fireworks", "Stones", "CustomItems");
         case 3:
            if (!values[2].isEmpty()) {
               ArrayList values2 = new ArrayList();
               StringUtil.copyPartialMatches(values[2], this.plugin.getSupportedMatNames(), values2);
               Collections.sort(values2);
               return values2;
            }

            return this.plugin.getSupportedMatNames();
         case 4:
            return Arrays.asList(
               "#A1672D&lWood",
               "&1&lArmor",
               "#1B22E8&lF#1BAAE8&li#1BE8E1&lr#1BE896&le#1BE829&lw#E8DA1B&lo#E88F1B&lr#EB0909&lk#EB09DC&ls",
               "&7&lStones",
               "#09EBDC&lCustom#89EB09&lItems"
            );
         case 5:
            return this.methods.getAvailableMainMenuSlots();
         default:
            return null;
      }
   }

   private void addSectionToSectionsConfig(Object value) {
      SendMessage.sendMessage(value, Lang.EDITSHOP_ADDING_SECTION.get().replace("%section%", this.section));
      this.plugin.getConfigManager().createSectionConfig(this.section);
      ConfigManager.getSection(this.section).set("enable", true);
      ConfigManager.getSection(this.section).set("item.material", this.material);
      ConfigManager.getSection(this.section).set("slot", this.slot);
      ConfigManager.getSection(this.section).set("item.displayname", this.displayname);
      ConfigManager.saveSection(this.section);
      this.reloadPerms();
      SendMessage.sendMessage(value, Lang.EDITSHOP_ADD_SECTION_SUCCESSFUL.get().replace("%path%", "/sections/" + this.section + ".yml"));
      SendMessage.sendMessage(value, Lang.EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES.get());
   }

   private void reloadPerms() {
      for (String shopId : new String[]{"shop", "sellall", "sellallitem", "sellallhand", "sellgui"}) {
         Permission permission = new Permission("OnyxShop." + shopId + "." + this.section, PermissionDefault.TRUE);
         if (!this.plugin.getServer().getPluginManager().getPermissions().contains(permission)) {
            this.plugin.getServer().getPluginManager().addPermission(permission);
         }
      }
   }
}
