package com.onyxshop.files;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.onyxshop.OnyxShop;
import com.onyxshop.files.lang.TranslatableRaw;
import com.onyxshop.methodes.SendMessage;
import com.onyxshop.providers.UserManager;
import com.onyxshop.util.BukkitMeta;
import com.onyxshop.util.ConfigUtil;
import com.onyxshop.util.ReflectUtil;
import com.onyxshop.util.exceptions.ConfigLoadException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public enum Lang {
   UPDATING_SHOP_SETTINGS("Validating shop configs..."),
   CREATING_LATEST_SHOPS(
      "Creating default shops for the latest version because there was no existing layout found matching server version %version%, please use a supported server version."
   ),
   DEFAULT_SHOPS_CREATED("Default shops layout compatible with %version% created at %path%"),
   LOADED_SHOP_CONFIGURATIONS("Completed loading %total% shop configs from /shops/"),
   DEFAULT_SECTIONS_CREATED("Default sections layout created at %path%"),
   LOADED_SECTION_CONFIGURATIONS("Completed loading %total% section configs from /sections/"),
   CREATING_NEW_SHOP_CONFIG(
      "Creating a new shops config for '%section%' because no shops config was found while there is a sections config. Not removed correctly?"
   ),
   CREATING_NEW_SECTION_CONFIG(
      "Creating a new sections config for '%section%' because no sections config was found while there is a shops config. Not removed correctly?"
   ),
   DISABLED_DUE_NO_VAULT("Disabled due to no Vault dependency found, please install Vault."),
   DISABLED_DUE_NO_ECONOMY("Disabled due to no economy plugin was found, please install a plugin such as EssentialsX!"),
   DISCOUNTS_FEATURE_UNAVAILABLE("Discounts feature cannot be enabled because no permissions plugin was found."),
   DATABASE_CONNECTED("Successfully connected to %database-type% database..."),
   UPDATE_AVAILABLE("There is an update available for OnyxShop, you are running v%plugin_version% but found v%latest_version%."),
   DONE("Done - Took %millis%ms to complete"),
   COMPLETED_DELAYED("Completed delayed loading"),
   UPDATING_SECTIONS_CONFIG("Updating sections config..."),
   DEBUG_MODE_ENABLED("Debug mode is enabled."),
   ECONOMY_PROVIDER_NULL("EconomyProvider is null."),
   LANGUAGE_FILE_USING("Using %languagefile% as language file."),
   CANNOT_FIND_LANGUAGE_FILE("Can not find language file!"),
   MINECRAFT_VERSION_USING("Using minecraft version %version%..."),
   LOADING_ITEMS("Loading all items..."),
   COULD_NOT_FIND_VALID_VERSION(
      "Could not find a valid implementation for this server version. This will cause the plugin to not have a version to use and result in lots of items/methods not working. Please use a supported minecraft version."
   ),
   UPDATING_CONFIGS("Updating the configurations because a newer version has been found..."),
   CREATING_BACKUP("Creating a backup from the old %fileName%, the file can be found in the backups folder inside the OnyxShop plugin's folder."),
   CANNOT_HOOK_PERMISSIONS("Failed to find a supported permissions plugin"),
   ENABLED_PLUGIN_HOOK("Enabled %plugin% hook..."),
   FAILED_PLUGIN_INTEGRATION("Failed to hook into %plugin%, plugin is disabled or not found..."),
   LOADING_SEASON_MODIFIERS("Loading season modifiers from config..."),
   LOADED_SEASON_MODIFIERS("Completed loading %total% modifier(s) for season '%season%' in world '%world%'"),
   NO_SEASON_MODIFIERS_FOUND("No season modifiers found for season %season% in config"),
   SEASON_CHANGE("Detected season change in world '%world%', loading modifiers for season %season%..."),
   ECONOMY_PROVIDERS_LOADED("Completed loading %total% economy provider(s) for all %shops% shop sections."),
   SECTION_ECONOMY_PROVIDER_LOADED("Using '%economy%' as default economy for shop '%shop%'"),
   PROVIDERS_NOT_ENABLED_YET("Providers not enabled yet, waiting..."),
   LOADED_SHOP_STAND_MODULE("Completed loading %count% shop stand(s) from stands.json"),
   SAVING_SHOP_STANDS("Saving %count% shop stands to 'stands.json'..."),
   SAVING_PRICE_MODIFIERS("Saving %count% unsaved player command modifiers to database..."),
   LOADING_LANGUAGE_FILES("Loading language files from LanguageFiles folder..."),
   FAILED_TO_LOAD_LANGUAGE_FILE("Failed to load language file '%fileName%', skipping..."),
   FAILED_TO_GET_LOCALE("Failed to get language for file name '%fileName%', skipping..."),
   LOADED_LANGUAGE_FILES("Successfully loaded %amount% language files from /LanguageFiles/ folder"),
   FAILED_TO_LOAD_LANGUAGE_FILES("Failed to load per player languages because LanguageFiles folder cannot be found"),
   ERROR_LOADING_BEDROCK_ICONS("Failed to load bedrock item icons with reason: %reason%"),
   CANNOT_GET_GEYSER_PALETTE("Could not get the latest Geyser item palette, icons for items in bedrock forms will not load!"),
   SUCCESSFULLY_LOADED_BEDROCK_ICONS("Successfully loaded %amount% item icon ids for bedrock forms"),
   LOADING_ITEM_IDS_FROM_GEYSER("Loading bedrock item ids from installed Geyser instance(%instanceVersion%)..."),
   ERROR_LOADING_ENCHANTMENT_COSTS("Failed to load enchantment variations because the enchant-costs.yml config cannot be read"),
   ENCHANTMENT_COSTS_LOADED("Successfully loaded %amount% enchantment costs from enchant-costs.yml"),
   INVENTORY_MAIN_SHOP_TITLE("&8&lServer Shop"),
   PROFILE("&aProfile"),
   NAME("&6Name"),
   MONEY("&6Money"),
   LEVEL("&6Level"),
   INVENTORY_SELLGUI_TITLE("&8&lSellGUI"),
   INVENTORY_BUYSTACKS_TITLE("&8Choose &lamount of stacks"),
   SHOPSEARCH_TITLE("&8&lSearch a item"),
   SHOPSEARCH_PRE_ENTERED_VALUE("Enter item name"),
   SHOPSEARCH_RESULT_TITLE("&8&lSearch result"),
   SHOPSEARCH_METHOD_BY_NAME("item name"),
   SHOPSEARCH_METHOD_BY_TYPE("item type"),
   SHOPSEARCH_CHANGE_SEARCH_METHOD("&6&lClick to search using: &c%search-method%"),
   INVENTORY_HOWMUCHSELL_TITLE("&8Choose &lsell amount"),
   INVENTORY_HOWMUCHBUY_TITLE("&8Choose &lbuy amount"),
   PLUS_ONE("&b+1"),
   MIN_ONE("&5-1"),
   PLUS_SIXTEEN("&b+16"),
   MIN_SIXTEEN("&5-16"),
   PLUS_THIRTY_TWO("&b+32"),
   MIN_THIRTY_TWO("&5-32"),
   SELL("&aClick to sell"),
   SELL_ALL("&cClick to sell all"),
   BUY("&aClick to purchase"),
   BUY_STACKS("&cClick to buy stacks"),
   RIGHT_CLICK_CHANGE_CURRENCY("&6Right Click: Change currency to &f%currency%"),
   CHANGE_CURRENCY("&6Change currency"),
   CURRENT_CURRENCY("&7Currently: &f%currency%"),
   SELECTED_AMOUNT("&aAmount"),
   ITEM_STOCK("&8Item stock: %stock%"),
   RESTOCKING_ITEM_STOCK("&8Item will be restocked in %time%"),
   OUT_OF_STOCK("&8Item stock: 0"),
   CURRENT_SELL_LIMIT("&8Sell limit: %limit%"),
   REACHED_SELL_LIMIT("&8You cannot sell this item anymore"),
   RESTOCKING_SELL_LIMIT("&8You may not sell this item for %time%"),
   MAX_AMOUNT_REACHED("&cMax amount reached"),
   MIN_AMOUNT_REACHED("&cMin amount reached"),
   SHOP_PREFIX("&9&lShop &8&l>> "),
   CONSOLE_PREFIX("&8[&3OnyxShop&8]&r"),
   ITEM_EDITOR_TITLE("&8&lItem editor"),
   CREATE_NEW_ITEM_IN_SHOP("&a&lCreate new item in this section."),
   EDIT_ITEM("&9&lEdit item."),
   DELETE_ITEM_IN_SHOP("&4&lDelete item"),
   BUY_PRICE("&aBuy price: &c%amounttopay%"),
   SELL_PRICE("&6Sell price: &c%amounttopay%"),
   OPTION_MATERIAL("material"),
   OPTION_DISPLAYNAME("displayname"),
   OPTION_NAME("name"),
   OPTION_BUY_PRICE("buy price"),
   OPTION_SELL_PRICE("sell price"),
   OPTION_LORE("lore"),
   OPTION_DISPLAYLORE("displaylore"),
   OPTION_PRICING_LORE("pricing lore"),
   OPTION_SLOT("slot"),
   OPTION_STACK_SIZE("stack size"),
   OPTION_ENCHANTMENT("enchantments"),
   OPTION_ENCHANTMENT_GLINT("enchantment-glint"),
   OPTION_STRENGTH("strength"),
   OPTION_SPAWNER_TYPE("spawnertype"),
   OPTION_POTIONTYPES("potiontypes"),
   OPTION_SKULLOWNER("skullowner"),
   OPTION_RECIPE("recipes"),
   OPTION_ARMORCOLOR("armorcolor"),
   OPTION_FIREWORK_DURATION("duration"),
   OPTION_FIREWORK_COLORS("colors"),
   OPTION_FIREWORK_FADE_COLORS("fade-colors"),
   OPTION_FIREWORK_FLICKER("flicker"),
   OPTION_FIREWORK_TRAIL("trail"),
   OPTION_FIREWORK_SHAPE("shape"),
   OPTION_BUY_COMMANDS("buy commands"),
   OPTION_SELL_COMMANDS("sell commands"),
   OPTION_NBTDATA("NBTData"),
   OPTION_COMPONENTS("components"),
   OPTION_GLOBAL_STOCK("global stock"),
   OPTION_GLOBAL_STOCK_LIMIT("global stock limit"),
   OPTION_GLOBAL_STOCK_RESTOCK_TIME("global restock time"),
   OPTION_GLOBAL_STOCK_RESTOCK_FREQUENCY("global restock frequency"),
   OPTION_PLAYER_STOCK("player stock"),
   OPTION_PLAYER_STOCK_LIMIT("player stock limit"),
   OPTION_PLAYER_STOCK_RESTOCK_TIME("player restock time"),
   OPTION_PLAYER_STOCK_RESTOCK_FREQUENCY("player restock frequency"),
   OPTION_LEVEL_REQUIREMENT("level requirement"),
   OPTION_SKULL_TEXTURE("skull texture"),
   OPTION_DYNAMIC_PRICING("dynamic pricing"),
   OPTION_DYNAMIC_PRICING_ENABLE("dynamic pricing"),
   OPTION_DYNAMIC_PRICING_MAX_STOCK("max stock"),
   OPTION_SUBSECTION("sub section"),
   ITEM_EDITOR_CHOOSE_INDEX_TITLE("&8&lChoose a value to edit:"),
   ITEM_EDITOR_AVAILABLE_OPTIONS_TITLE("&8&lChoose an option:"),
   EDIT_OPTION_OF_ITEM("&6Edit %option%"),
   TOGGLE_OPTION_OF_ITEM("&6Toggle %option%"),
   DISABLED_OPTION("Option %option% is disabled: \n&cThe material of the item is not capable \n&cof having option %option%."),
   DISABLED_POTIONTYPES_OPTION(
      "&6Option potiontypes is disabled: \n&cThe material of the item is not a potion, \n&csplash potion, lingering potion or tipped arrow."
   ),
   DISABLED_SPAWNER_TYPE_OPTION("&6Option spawnertype is disabled: \n&cThe material of the item is not a spawner."),
   DISABLED_RECIPE_OPTION("&6Option recipe is disabled: \n&cThe material of the item is not a knowledge-book."),
   DISABLED_SKULLOWNER_OPTION("&6Option skullowner is disabled: \n&cThe material of the item is not a player-head."),
   DISABLED_ARMORCOLOR_OPTION("&6Option armorcolor is disabled: \n&cThe material of the item is no leather armor."),
   DISABLED_FIREWORK_OPTIONS("&6The firework options can't be edited: \n&cThe material of the item is no firework rocket."),
   DISABLED_DYNAMIC_PRICING_OPTION("&cThe dynamic pricing setting in the config.yml has to be enabled in order to change per-item dynamic pricing settings."),
   DISABLED_OPTION_CONFLICT("&6The %option% can't be edited: \n&cCannot edit %option% because \n&c%option2% is already enabled."),
   DISABLED_OPTION_ACTION_ITEM("&6The %option% can't be edited \n&6on a action item. \n&cRemove the action from this item \n&cto start editing &f%option%"),
   CURRENT_VALUE("&9Current %option%: "),
   FIREWORK_OPTIONS("firework options"),
   DYNAMIC_PRICING_OPTIONS("dynamic pricing options"),
   GLOBAL_STOCK_OPTIONS("global stock options"),
   PLAYER_STOCK_OPTIONS("player stock options"),
   SELL_LIMIT_OPTIONS("sell limit options"),
   ITEM_EDITOR_FIREWORK_TITLE("&8&lEdit firework options"),
   ITEM_EDITOR_GLOBAL_STOCK_TITLE("&8&lEdit global stock options"),
   ITEM_EDITOR_PLAYER_STOCK_TITLE("&8&lEdit player stock options"),
   ITEM_EDITOR_SELL_LIMIT_TITLE("&8&lEdit sell limit options"),
   ITEM_EDITOR_DYNAMIC_PRICING_TITLE("&8&lEdit dynamic pricing options"),
   ITEM_EDITOR_CHOOSE_VALUE_TITLE("&8&lChoose an value:"),
   ANVILGUI_ENTER_VALUE("&8&lEnter the %option%"),
   ITEM_EDITOR_CONCLUSION_TITLE("&8&lEdit successful"),
   STATUS("Status:"),
   ITEM_CREATED("&a&lThe item has been \n&a&ladded to the shop."),
   ITEM_EDITED("&a&lThe item has been edited."),
   ITEM_DELETED_FROM_SHOP("&a&lThe item has been \n&a&lremoved from shop."),
   ITEM_EDITOR_DELETE_ITEM_CONFIRMATION_TITLE("&8&lAre you sure?"),
   CONFIRM_DELETE_ITEM("&4&lYes, delete item."),
   CANCEL_DELETE_ITEM("&a&lNo, do not delete this item."),
   ITEM_EDITOR_AVAILABLE_ACTIONS_TITLE("&8&lChoose a action:"),
   REMOVE_ACTION("&a&lRemove the %key%."),
   EDIT_ACTION("&a&lEdit the existing %key%."),
   ADD_ACTION("&a&lAdd the %key%."),
   DISABLED_ACTION("&c&lThis action cannot be executed."),
   NAME_PROMPT_TEXT("&aEnter a name for the item, you can use color codes with '&'. \nType 'exit' to exit."),
   LORE_PROMPT_TEXT("&aEnter the lore, you can use color codes with '&'. \nType 'exit' to exit."),
   COMMAND_PROMPT_TEXT("&aEnter the command, you can use %player% to get the player name that bought the command. \nType 'exit' to exit."),
   NBTDATA_PROMPT_TEXT("&aEnter the NBTData. \nType 'exit' to exit."),
   RESTOCK_FREQUENCY_PROMPT_TEXT("&aEnter a cron expression for when to restock this item.\nType 'exit' to exit."),
   EXITED_CONVERSATION("&aExited the conversation."),
   UPDATE_DETAILS(
      "&aYou are about to download version &c%version% &awith update data: \n&6Title: &e %title% \n&6Release date: &e%timestamp% \n&6Changelog: \n&e%changelog%"
   ),
   DISCORD_VALIDATION_REQUIRED("&cValidation is required to continue, click here to validate via discord."),
   NO_NEW_VERSION_FOUND("&aThere were no new versions found, you are already up-to-date!"),
   UNABLE_TO_CHECK_FOR_UPDATES("&cUnable to check for updates at this time"),
   VALIDATION_SUCCESSFUL("&aValidation successful, downloading update..."),
   VALIDATION_EXPIRED("&cValidation process for update download expired"),
   FAILED_TO_DELETE_OLD_VERSION("&cFailed to delete version %version%, you will have to remove the file manually from your plugins folder."),
   UPDATE_DOWNLOAD_SUCCESS_RESTART("&aUpdate download successful, restart your server for it to take affect."),
   UPDATE_DOWNLOAD_SUCCESS_RELOADING("&aUpdate download successful, reloading plugin..."),
   UPDATE_DOWNLOAD_FAILED("&cUpdate download failed, see console logs for details."),
   LOADING_DYNAMIC_PRICING("Loading DynamicPricing provider..."),
   SAVING_DYNAMIC_PRICING("Saving DynamicPricing to database..."),
   SAVED_DYNAMIC_PRICING("Took %millis%ms to save DynamicPricing to database"),
   RELOADING_DYNAMIC_PRICING("Reloading DynamicPricing from database..."),
   DYNAMIC_PRICING_PRICE_CHANGE("Detected price change for item '%path%' from shop %shop%, resetting DynamicPricing for this item..."),
   DYNAMIC_PRICING_MAX_STOCK_CHANGE("Detected DynamicPricing max stock change, resetting DynamicPricing for item '%path%' from shop %shop%..."),
   LOADING_ITEM_STOCK("Loading item stock provider..."),
   RELOADING_ITEM_STOCK("Reloading item stock provider..."),
   RESTOCKED_GLOBAL_ITEM_STOCK("Item '%path%' has been restocked successfully for global item stock"),
   RESTOCKING_GLOBAL_ITEM_STOCK("Starting the global restock timer for item '%path%'"),
   RESTOCKED_PLAYER_ITEM_STOCK("Item '%path%' has been restocked successfully for player '%playerUUID%'"),
   RESTOCKING_PLAYER_ITEM_STOCK("Starting the player restock timer for item '%path%' for player '%playerUUID%'"),
   PLAYER_ITEM_STOCK_CHANGE("Detected player stock change for item '%path%', updating item stock for all players..."),
   GLOBAL_ITEM_STOCK_CHANGE("Detected global stock change for item '%path%', updating item stock..."),
   LOADING_LIMITED_SELL("Loading limited selling provider..."),
   RELOADING_LIMITED_SELL("Reloading limited sell provider..."),
   RESTOCKED_GLOBAL_SELL_LIMIT("Item '%path%' has been restocked successfully for global sell limits"),
   RESTOCKING_GLOBAL_SELL_LIMIT("Starting the global sell restock timer for item '%path%'"),
   RESTOCKED_PLAYER_SELL_LIMIT("Item '%path%' has been restocked successfully for player sell limit for player '%playerUUID%'"),
   RESTOCKING_PLAYER_SELL_LIMIT("Starting the player sell restock timer for item '%path%' for player '%playerUUID%'"),
   PLAYER_SELL_LIMIT_CHANGE("Detected player sell limit change for item '%path%', updating item limit for all players..."),
   GLOBAL_SELL_LIMIT_CHANGE("Detected global sell limit change for item '%path%', updating item limit..."),
   SHOPSTAND_BUY_SCREEN_TITLE("&8Buying &a&l> &r&8%item%"),
   SHOPSTAND_SELL_SCREEN_TITLE("&8Selling &c&l> &r&8%item%"),
   SHOPSTAND_CLICK_TO_BUY("&aClick to buy %amount%"),
   SHOPSTAND_CLICK_TO_SELL("&aClick to sell %amount%"),
   SHOPSTAND_TOTAL_PRICE("&cTotal price: &7%price%"),
   DEFAULT_SHOP_STAND_ITEM_NAME("&eShopStand"),
   DEFAULT_SHOP_STAND_ITEM_LORE("&fPlace this item down to create\n&fa new &6ShopStand &fat the placed location\n&ffor shop item &c%item%"),
   SHOP_STANDS_MANAGEMENT_INVENTORY_TITLE("&8&lStand management"),
   SHOP_STAND_INFO_ITEM_NAME("&6&lShop stand info:"),
   SHOP_STAND_INFO_ITEM_LORE("&aStand ID: &f%id%\n&aLocation: &f%location%\n&aShop item: &f%item%\n&aStand type: &f%type%\n&aIs loaded: &f%loaded%"),
   TOGGLE_STAND_HOLOGRAM_ITEM_NAME("&a&lToggle hologram"),
   TOGGLE_STAND_HOLOGRAM_ITEM_LORE("&fClick to &c%action% &fthe hologram of this shop stand"),
   FORCE_RELOAD_STAND_ITEM_NAME("&e&lForce reload stand"),
   FORCE_RELOAD_STAND_ITEM_LORE("&fClicking this will reload the shop stand\n&fwhich will re-create the displayitem and stand."),
   DESTROY_STAND_ITEM_NAME("&4&lDestroy shop stand"),
   DESTROY_STAND_ITEM_LORE("&eClick to destroy this shop stand"),
   RELOADED_SHOP_STAND("&aForcefully reloaded shop stand with ID %id%"),
   SHOP_STAND_BROWSE_INVENTORY_TITLE("&8&lStand list"),
   SHOP_STAND_LOCATION_FORMAT("World '%world%', x%pos_x%, y%pos_y%, z%pos_z%"),
   CREATED_NEW_SHOP_STAND("&aSuccessfully created new ShopStand for &f%item%"),
   ERROR_CREATING_NEW_SHOP_STAND("&cFailed to create a new shop stand: %reason%"),
   ENABLED_ROTATING_SHOP("Successfully enabled rotating shop %shop%, with a rotating interval of %interval%"),
   ENABLED_ROTATING_SHOP_FREQUENCY(
      "Successfully enabled rotating shop %shop%, with a rotating frequency of %frequency% which will next occur at %nextRotation%"
   ),
   ENABLED_ROTATING_SHOP_SYNC("Successfully enabled rotating shop %shop%, syncing its rotation items from database..."),
   ROTATING_SHOP_FORCE_ROTATE_BLOCKED(
      "Cannot force a rotation for shop %shop% because it syncs its rotation items from the database, force rotate on the server that owns the schedule instead"
   ),
   LOADING_NEXT_ROTATION("Loading next rotation items for shop %shop%"),
   NEW_ITEMS_GLOBAL_MESSAGE("&aNew items were just added to %shopName%&r&a, check out &c/shop %shop%&a. Next items in &f%interval%"),
   FAILED_LOADING_NEXT_ROTATION("Error occurred while loading next rotation for shop %shop%"),
   INVALID_ROTATION_INTERVAL("Invalid interval for rotating shop %shop% with format: %interval%"),
   INVALID_DISPLAY_CHANCES("Invalid chances for items in shop %shop%, should be a maximum sum of 100 but got %sum%"),
   CANNOT_LOAD_DISPLAY_SLOT(
      "Cannot load display slot %slot% in page %page% for rotating shop %shop% because the slot is occupied by a shop item using the 'slot' option"
   ),
   PREVIOUS_PAGE("&dPrevious Page"),
   NEXT_PAGE("&bNext Page"),
   CURRENT_PAGE("&7Page:"),
   CANCEL("&4Cancel"),
   BACK("&4Back"),
   LEFT_CLICK_BUY("\n &a&lBuy Price\n &a■ &f%buyPrice%"),
   DYNAMIC_BUY_PRICE("\n &a&lBuy Price\n &a■ &f&m%buyPrice%&r %dpp%&f%dynamicBuyPrice%"),
   SEASONAL_BUY_PRICE("\n &a&lBuy Price\n &a■ &f&m%buyPrice%&r %dpp%&f%seasonalBuyPrice%"),
   DISCOUNTED_BUY_PRICE("\n &a&lBuy Price\n &a■ &f&m%buyPrice%&r %dpp%&f%discountedPrice%"),
   RIGHT_CLICK_SELL("\n &c&lSell Price\n &c■ &f%sellPrice%"),
   DYNAMIC_SELL_PRICE("\n &c&lSell Price\n &c■ &f&m%sellPrice%&r %dpp%&f%dynamicSellPrice%"),
   SEASONAL_SELL_PRICE("\n &c&lSell Price\n &c■ &f&m%sellPrice%&r %dpp%&f%seasonalSellPrice%"),
   MULTIPLIED_SELL_PRICE("\n &c&lSell Price\n &c■ &f&m%sellPrice%&r %dpp%&f%multipliedPrice%"),
   MIDDLE_CLICK_SELL_ALL("&bMiddle Click: Sell All"),
   SHIFT_RIGHT_CLICK_SELL_ALL("&bShift + Right Click: Sell All"),
   RISING_DYNAMIC_PRICE_PREFIX("&a⏶%percent%% "),
   DROPPING_DYNAMIC_PRICE_PREFIX("&c⏷%percent%% "),
   CURRENT_ITEM_STOCK("&fCurrent item stock: &a%itemStock%"),
   RESTOCKING_ITEM("&fItem will be restocked in: &a%time%"),
   ITEM_OUT_OF_STOCK("&cItem is out of stock"),
   SELL_LIMIT("&cSell limit: &6%limit%"),
   SELL_LIMIT_REACHED("&cYou cannot sell this item anymore"),
   SELL_LIMIT_RESTOCKING("&cYou may not sell this item for &6%time%"),
   SHIFT_CLICK_ITEM_EDITOR("&eShift Click: Edit item"),
   ITEM_LOCATION_IN_SHOP("&8Section: %shopsection%, Index: %itemLoc%"),
   ITEM_CANNOT_BE_BOUGHT("\n &a&lBuy Price\n &a■ &fCannot be bought"),
   ITEM_CANNOT_BE_SOLD("\n &c&lSell Price\n &c■ &fCannot be sold"),
   ITEM_SEASONAL_PRICE("&e&lSeasonal price %season-icon%"),
   DECORATION_LORE("\n &e&lUsage:"),
   DECORATION_LORE_BUY("&e ╚&e&l» &fLeft click to buy"),
   DECORATION_LORE_SELL_ALL("&e ╚&e&l» &fShift + Right Click to sell all"),
   DECORATION_LORE_SELL("&e ╚&e&l» &fRight click to sell\n "),
   DECORATION_LORE_VIEW("&e ╚&e&l» &fClick to view\n "),
   LEVEL_REQUIREMENT_LORE("&c&lRequires at least a experience level of %requirement%."),
   PLAYTIME_REQUIREMENT_LORE("&c&lRequires at least a playtime of %requirement%."),
   PLACEHOLDER_EQUALS_REQUIREMENT_LORE("&c&lRequires %requirement%"),
   PLACEHOLDER_DIFFERS_REQUIREMENT_LORE("&c&lDifferent %requirement% required"),
   PLACEHOLDER_LARGER_REQUIREMENT_LORE("&c&lRequires more %requirement%"),
   PLACEHOLDER_SMALLER_REQUIREMENT_LORE("&c&lRequires less %requirement%"),
   TIME_REQUIREMENT_LORE("&c&lCan only be purchased between\n&c%minTime% &land &c%maxTime% &lingame time."),
   QUEST_REQUIREMENT_LORE("&c&lRequires to complete quest &b&l%questName%"),
   REGION_REQUIREMENT_LORE("&c&lRequires to be inside region &b&l%regionName%"),
   PERMISSION_REQUIREMENT_LORE("&c&lRequires the permission &f%requirement%"),
   ITEM_REQUIREMENT_LORE("&c&lRequires to have %amount%x %itemName% in inventory"),
   DISCORD_SRV_PLAYER_TRANSACTION("%player_name% %bought/sold% %amount% x %items% for %price%"),
   SELL_CONFIRMATION_MULTIPLE_ITEMS("&aYou have successfully sold &1%amount% &aitems for &c%amounttopay%"),
   SELL_CONFIRMATION("&aYou have successfully sold &1%amount% x &b%material% &afor &c%amounttopay%"),
   SELL_CONFIRMATION_COMMAND("&aYou have successfully sold an &6command &afor &c%amounttopay% &aand is now run."),
   PAY_CONFIRMATION("&aYou have successfully bought &1%amount% x &b%material% &afor &c%amounttopay%"),
   PAY_CONFIRMATION_COMMAND("&aYou have successfully bought an &6command &afor &c%amounttopay% &aand is now run."),
   NO_ITEM_TO_BE_SOLD("&cYou do not have any of this item inside your inventory."),
   HIGHER_LEVEL_REQUIRED("&cYou need to be atleast level %level-required% for this item, you are level %level%"),
   ALREADY_PURCHASED_ITEM("&cYou already purchased this, it is not possible to purchase it again."),
   LIMITED_ITEM_SELL_AMOUNT("&cYou are not able to sell this item at this time"),
   LEVEL_REQUIREMENT_NOT_MET("&cThis item has a level requirement of %requirement%, &cyou are currently level %progress%."),
   PLAYTIME_REQUIREMENT_NOT_MET("&cThis item requires you to have a playtime of %requirement%, &cyou currently have a playtime of %progress%."),
   PLACEHOLDER_EQUALS_REQUIREMENT_NOT_MET("&cThis item requires you to have %requirement%, &cyou currently have %progress%."),
   PLACEHOLDER_DIFFERS_REQUIREMENT_NOT_MET("&cThis item requires a different %requirement%, &cyou currently have %progress%."),
   PLACEHOLDER_LARGER_REQUIREMENT_NOT_MET("&cThis item requires atleast %requirement%, &cyou currently have %progress%."),
   PLACEHOLDER_SMALLER_REQUIREMENT_NOT_MET("&cThis item requires less %requirement%, &cyou currently have %progress%."),
   TIME_REQUIREMENT_NOT_MET(
      "&cThis item can only be bought/sold between &f%minTime% &cand &f%maxTime%&c ingame time, current time is &f%progress%&c. Come back shortly!"
   ),
   QUEST_REQUIREMENT_NOT_MET("&cThis item can only be bought/sold when you completed the quest &b&l%questName%&c, come back once you completed it"),
   REGION_REQUIREMENT_NOT_MET("&cYou may only buy/sell this item inside the region %regionName%"),
   PERMISSION_REQUIREMENT_NOT_MET("&cThis item requires the %requirement% permission"),
   ITEM_REQUIREMENT_NOT_MET("&cYou need to have %amount%x %itemName% in your inventory"),
   PLAYER_MADE_TRANSACTION("%playername% %bought/sold% %amount% x %material% for %amountofmoney% with the %buy/sell-method%."),
   PLAYER_MADE_TRANSACTION_MULTIPLE_ITEMS("%playername% %bought/sold% %items% for %amountofmoney% with the %buy/sell-method%."),
   TRANSACTION_FAILED_NO_SPACE(
      "%playername% tried to make a transaction of %amount% x %material% for %amountofmoney% but the transaction failed because there was not enough space inside the inventory."
   ),
   BUY_SCREEN("buy screen"),
   SELL_SCREEN("sell screen"),
   BUYSTACKS_SCREEN("buy stacks screen"),
   SELLALL_SCREEN("sell all screen"),
   SELLALL_COMMAND("sell all command"),
   SELLGUI_SCREEN("sell gui"),
   QUICK_BUY("quick buy screen"),
   QUICK_SELL("quick sell screen"),
   SHOPSTAND_BUY_SCREEN("shopstand buy screen"),
   SHOPSTAND_SELL_SCREEN("shopstand sell screen"),
   AUTO_SELL_CHEST("SellChest"),
   BOUGHT("bought"),
   SOLD("sold"),
   TRANSACTION_SCREEN_TOTAL_PRICE("&3%price%"),
   TRANSACTION_SCREEN_TOTAL_AMOUNT("&5x %amount%"),
   CANNOT_PURCHASE_ITEM("&cThis item cannot be purchased."),
   CANNOT_SELL_ITEM("&cThis item cannot be sold."),
   ON_SCREEN_TITLE_SOLD("&a+%price%"),
   ON_SCREEN_TITLE_BOUGHT("&c-%price%"),
   CURRENCYSYMBOL("$%price%"),
   SYMBOL_PRICING_FORMAT("%symbol%%price%"),
   NAMED_PRICING_FORMAT("%price% %currency-format%"),
   AND_PRICE_SEPARATOR_LONG(" and "),
   AND_PRICE_SEPARATOR_SHORT(", "),
   OR_PRICE_SEPARATOR_LONG(" or "),
   OR_PRICE_SEPARATOR_SHORT(", "),
   XP_CURRENCY_NAME_SINGULAR("XP point"),
   XP_CURRENCY_NAME_PLURAL("XP points"),
   LEVELS_CURRENCY_NAME_SINGULAR("level"),
   LEVELS_CURRENCY_NAME_PLURAL("levels"),
   TIME_FORMAT_MONTHS("mo"),
   TIME_FORMAT_WEEKS("w"),
   TIME_FORMAT_DAYS("d"),
   TIME_FORMAT_HOURS("h"),
   TIME_FORMAT_MINUTES("m"),
   TIME_FORMAT_SECONDS("s"),
   COMMAND_DISABLED_IN_WORLD("&cThis command is disabled inside this world."),
   EDITSHOP_ADD_ITEM_SUBCOMMAND_DESC("&aAdd a item to the shop."),
   EDITSHOP_EDIT_ITEM_SUBCOMMAND_DESC("&aEdit an existing item in the shop."),
   EDITSHOP_REMOVE_ITEM_SUBCOMMAND_DESC("&aDelete a item from the shop."),
   EDITSHOP_IMPORT_SUBCOMMAND_DESC("&aImports files/settings from another location or plugin."),
   EDITSHOP_ADD_HAND_ITEM_SUBCOMMAND_DESC("&aAdd a item from you hand to the shop."),
   EDITSHOP_SET_GLOBALSTOCK_COMMAND_DESC("&aSets the global stock limit for a shop item"),
   EDITSHOP_RESET_GLOBALSTOCK_COMMAND_DESC("&aResets the stock limit for specific shop item(s)/section(s)"),
   EDITSHOP_ADD_GLOBALSTOCK_COMMAND_DESC("&aAdds global stock for the shop item"),
   EDITSHOP_SET_PLAYERSTOCK_COMMAND_DESC("&aSets the stock modifier for a specific player for a shop item"),
   EDITSHOP_RESET_PLAYERSTOCK_COMMAND_DESC("&aResets the stock limit for specific player(s) for shop item(s)/section(s)"),
   EDITSHOP_ADD_PLAYERSTOCK_COMMAND_DESC("&aAdds stock for the specified player for a shop item"),
   EDITSHOP_SET_GLOBALSELL_COMMAND_DESC("&aSets the sell limit for a specific shop item"),
   EDITSHOP_RESET_GLOBALSELL_COMMAND_DESC("&aResets the sell limit for specific shop item(s)/section(s)"),
   EDITSHOP_SET_PLAYERSELL_COMMAND_DESC("&aSets the sell limit for a specific player for a shop item"),
   EDITSHOP_RESET_PLAYERSELL_COMMAND_DESC("&aResets the sell limit for specific player(s) for shop item(s)/section(s)"),
   EDITSHOP_ADD_SECTION_SUBCOMMAND_DESC("&aAdd a shop section to the shop."),
   EDITSHOP_EDIT_SECTION_SUBCOMMAND_DESC("&aEdit an existing shop section."),
   EDITSHOP_REMOVE_SECTION_SUBCOMMAND_DESC("&aRemoves an existing shop section from the shop."),
   EDITSHOP_UPDATE_SUBCOMMAND_DESC("&aSearches for new updates of the plugin"),
   EDITSHOP_RESET_DYNAMICPRICING_SUBCOMMAND_DESC("&aReset the DynamicPricing for a specific item or all items at the same time"),
   EDITSHOP_MIGRATE_SUBCOMMAND_DESC("&aAllows to migrate data from another location or plugin."),
   EDITSHOP_SHOPSTANDS_SUBCOMMAND_DESC("&aManage existing or create new shop stands"),
   EDITSHOP_SHOPSTANDS_GIVE_SUBCOMMAND_DESC("&aGives a new shop stand item to place down"),
   EDITSHOP_SHOPSTANDS_DESTROY_SUBCOMMAND_DESC("&aDestroys a shop stand at its current location from its ID"),
   EDITSHOP_SHOPSTANDS_BROWSE_SUBCOMMAND_DESC("&aOpens a GUI with all existing shop stands"),
   EDITSHOP_ADD_ITEM_SUBCOMMAND_SYNTAX("&a/editshop additem <section> <material> <buy price> <sell price> [displayname]"),
   EDITSHOP_EDIT_ITEM_SUBCOMMAND_SYNTAX("&a/editshop edititem <section> <index> <action> <key> <value>"),
   EDITSHOP_REMOVE_ITEM_SUBCOMMAND_SYNTAX("&a/editshop deleteitem <section> <index>"),
   EDITSHOP_IMPORT_SUBCOMMAND_SYNTAX("&a/editshop import <plugin> <file>"),
   EDITSHOP_ADD_HAND_ITEM_SUBCOMMAND_SYNTAX("&a/editshop addhanditem <section> <buy price> <sell price>"),
   EDITSHOP_SET_GLOBALSTOCK_COMMAND_SYNTAX("&a/editshop globalstock set <section> <index> <amount>"),
   EDITSHOP_RESET_GLOBALSTOCK_COMMAND_SYNTAX("&a/editshop globalstock reset <section> <index>"),
   EDITSHOP_ADD_GLOBALSTOCK_COMMAND_SYNTAX("&a/editshop globalstock add <section> <index> <amount>"),
   EDITSHOP_SET_PLAYERSTOCK_COMMAND_SYNTAX("&a/editshop playerstock set <player> <section> <index> <amount>"),
   EDITSHOP_RESET_PLAYERSTOCK_COMMAND_SYNTAX("&a/editshop playerstock reset <player> <section> <index>"),
   EDITSHOP_ADD_PLAYERSTOCK_COMMAND_SYNTAX("&a/editshop playerstock add <player> <section> <index> <amount>"),
   EDITSHOP_SET_GLOBALSELL_COMMAND_SYNTAX("&a/editshop globalsell set <section> <index> <amount>"),
   EDITSHOP_RESET_GLOBALSELL_COMMAND_SYNTAX("&a/editshop globalsell reset <section> <index>"),
   EDITSHOP_SET_PLAYERSELL_COMMAND_SYNTAX("&a/editshop playersell set <player> <section> <index> <amount>"),
   EDITSHOP_RESET_PLAYERSELL_COMMAND_SYNTAX("&a/editshop playersell reset <player> <section> <index>"),
   EDITSHOP_ADD_SECTION_SUBCOMMAND_SYNTAX("&a/editshop addsection <section> <material> <displayname> <place>"),
   EDITSHOP_EDIT_SECTION_SUBCOMMAND_SYNTAX("&a/editshop editsection <section> <action> <key> <value>"),
   EDITSHOP_REMOVE_SECTION_SUBCOMMAND_SYNTAX("&a/editshop deletesection <section>"),
   EDITSHOP_UPDATE_SUBCOMMAND_SYNTAX("&a/editshop update"),
   EDITSHOP_RESET_DYNAMICPRICING_SUBCOMMAND_SYNTAX("&a/editshop dynamicpricing reset <all/section> [index]"),
   EDITSHOP_MIGRATE_SUBCOMMAND_SYNTAX("&a/editshop migrate <plugin> <image>"),
   EDITSHOP_SHOPSTANDS_SUBCOMMAND_SYNTAX("&a/editshop shopstands <give/destroy> ..."),
   EDITSHOP_SHOPSTANDS_GIVE_SUBCOMMAND_SYNTAX("&a/editshop shopstands give <standType> <section> <index>"),
   EDITSHOP_SHOPSTANDS_DESTROY_SUBCOMMAND_SYNTAX("&a/onyx shopstands destroy <standID>"),
   EDITSHOP_SHOPSTANDS_BROWSE_SUBCOMMAND_SYNTAX("&a/onyx shopstands browse"),
   EDITSHOP_ADDING_ITEM("&aAdding new item to shop %shop%..."),
   EDITSHOP_ADD_ITEM_SUCCESSFUL("&aSuccessfully added the item to shop %shop% under %itemPath%."),
   EDITSHOP_ADDING_SECTION("&aAdding new section config for shop %section%..."),
   EDITSHOP_ADD_SECTION_SUCCESSFUL("&aNew section config created at %path%."),
   EDITSHOP_EDITING_ITEM("&aUpdating the item..."),
   EDITSHOP_EDITING_SECTION("&aUpdating section..."),
   EDITSHOP_EDIT_ITEM_SUCCESSFUL("&aSuccessfully updated '%itemPath%'."),
   EDITSHOP_REMOVING_ITEM("&aRemoving the item from the shop..."),
   EDITSHOP_REMOVE_ITEM_SUCCESSFUL("&aSuccessfully removed '%itemPath%' from the shop."),
   EDITSHOP_REMOVING_SECTION("&aRemoving the section from the shop..."),
   EDITSHOP_REMOVED_SECTION("&a&aSuccessfully removed '%itemPath%' from shop."),
   EDITSHOP_RELOAD_SHOP_TO_SEE_CHANGES("&aUse command /sreload to load the changes inside the shop."),
   FILE_REPLACED("&aThe old %fileName% has been replaced with the new file, the old file can be found inside the backups folder if you messed up something."),
   GLOBALSTOCK_SET_SUCCESSFUL("&aSuccessfully updated the global stock limit for item '%itemPath%'"),
   STOCK_LIMIT_RESET_SUCCESSFUL("&aSuccessfully reset the stock limit, took %time%ms to complete"),
   PLAYERSTOCK_SET_SUCCESSFUL("&aSuccessfully updated the player stock limit for player '%player%' for item '%itemPath%'"),
   PLAYERSTOCK_SET_ALL_PLAYERS("&aSuccessfully updated the player stock limit for all players for item '%itemPath%'"),
   GLOBALSELL_SET_SUCCESSFUL("&aSuccessfully updated the global sell limit for item '%itemPath%'"),
   PLAYERSELL_SET_SUCCESSFUL("&aSuccessfully updated the player sell limit for player '%player%' for item '%itemPath%'"),
   PLAYERSELL_SET_ALL_PLAYERS("&aSuccessfully updated the player sell limit for all players for item '%itemPath%'"),
   RESETTING_DYNAMICPRICING("&aResetting DynamicPricing for all items..."),
   DYNAMICPRICING_RESET_SPECIFIC_ITEM("&aSuccessfully reset the DynamicPricing for item '%itemPath%'"),
   DYNAMICPRICING_RESET("&aSuccessfully reset the DynamicPricing for all items"),
   SHOP_STAND_ITEM_RECEIVED("&aYou were given &c%amount%x &aShopStand for shop item &f%item%"),
   CHECK_PLAYER_PRICE_MODIFIERS("&aPlayer &6%player_name% &ahas &b%amount% &aprice modifier(s):"),
   PLAYER_PRICE_MODIFIER_REMOVED("&aRemoved &f%location% &amodifier for &6%player_name%&a(&6%player_uuid%&a) for &f%type% &aprice"),
   PLAYER_PRICE_MODIFIER_SET("&aSuccessfully set &f%location% &amodifier for &6%player_name%&a(&6%player_uuid%&a) for &f%type% &aprice to &b%amount%%"),
   SECTION_DOES_NOT_CONTAIN_ITEM("&cSection '%shopsection%' does not contain the item '%itemLoc%'."),
   NO_VALID_ACTION("&cNot a valid action '%action%'. \nAvailable actions: %actions%."),
   NO_VALID_KEY("&cNot a valid key '%key%'. \nAvailable keys: %keys%."),
   ACTION_NOT_VALID_ON_KEY("&cCannot %action% the '%key%' key."),
   EDIT_KEY_UNAVAILABLE("&cCannot edit the '%key%' of this item as it is a action item"),
   EDIT_ITEM_MATERIAL_INVALID("&cThe material from the item you are trying to edit is invalid. '%material%'"),
   KEY_ALREADY_DEFINED("&cThe '%key%' key is already defined on the item, you cannot add this twice."),
   KEY_IS_NOT_FOUND("&cThe '%key%' key cannot be removed from the item because it is not found."),
   PLUGIN_NOT_FOUND("&cCannot find a plugin specified with that name, please check if we do support this plugin."),
   FILE_NOT_FOUND("&cCannot find a file with name %fileName% inside the plugin folder of %pluginName%."),
   NO_VALID_SLOT_FOR_ITEM("&cThe item could not be placed at slot '%slot%'. The slot needs to be 1 - %maxShopSize%."),
   SLOT_ALREADY_IN_USE("&cThe slot for this section is already taken.\nAll available slots: %available-slots%"),
   SLOT_NEEDS_TO_BE_NUMBER("&cThe slot of the item in the main menu needs to be a number."),
   INVALID_SHOP_STAND_TYPE("&cInvalid shop stand type, valid values: %values%"),
   UNKNOWN_SHOP_STAND_ID("&cNo shop stand found with ID &f%id%"),
   STAND_TYPE_NOT_SUPPORTED("&cShop stand type &f%type% &cis not supported in the current minecraft version"),
   NO_PLAYER_PRICE_MODIFIERS("&cNo modifiers found for this player"),
   NO_PLAYER_PRICE_MODIFIERS_FOR_FILTERS("&cNo modifiers found for this player for the given filters"),
   CONFIGS_RELOADED("&aAll configs reloaded!"),
   SELLALL_COMMAND_USAGES("&aCommand usage(s):"),
   SELLALL_INVENTORY_COMMAND_USAGE("&7/sellall inventory &f- &7Sells all items in your inventory"),
   SELLALL_HAND_COMMAND_USAGE("&7/sellall hand &8[amount] &f- &7Sells all items in your hand"),
   SELLALL_ITEM_COMMAND_USAGE("&7/sellall <item> &8[amount] &f- &7Sells all items in your inventory matching the given item name"),
   SHOP_ITEM_GIVEN("&aYou were given &c%amount% &ax &6%material%"),
   SHOP_ITEM_RECEIVED("&aYou received &c%amount% &ax shop item from section &6%section% &aand item index &6%itemIndex%"),
   MINIMUM_AMOUNT("&cThe minimum amount to pay a player is %minimumamount%."),
   PAYMENT_COMPLETE("&aPayment complete."),
   PLAYER_RECEIVED_MONEY("&aYou received &c%amounttogive% &afrom &2%playername%&a!"),
   NEED_PLAYER_AND_AMOUNT("&9You need to give a player and a amount of money."),
   NEED_AMOUNT("&9You need to give a amount of money."),
   PAYCOMMAND_EXAMPLE("&9/pay <player> <amount>"),
   GIVEMONEYCOMMAND_EXAMPLE("&9/givemoney <player> <amount>"),
   GIVEMONEY_COMPLETE("&aGivemoney complete."),
   CHECK_BALANCE("&aYou currently have &b%amountofmoney%&a."),
   CHECK_BALANCE_OTHER_PLAYER("&b%playername% &acurrently has &c%amountofmoney%&a."),
   DEFAULT_SPAWNER_NAME("&9&l%spawner-type% &rSpawner"),
   SPAWNER_BROKE_AND_APPLIED_TO_INVENTORY("&aYou successfully mined a &9%spawner-type%&a, the item is applied to your inventory."),
   NEED_SILK_TOUCH("&cYou need to use an silk touch pickaxe to mine this!"),
   JOIN_MESSAGE("&2Welcome, your balance is &c%balance%!"),
   LEVEL_EVENT_MESSAGE("&2[Levels] &5Here you go, &c%amount% &5has been added to your balance!"),
   DISPLAYNAME_NULL("Could not get displayname."),
   ITEM_NAME_NULL("Could not get the name of the item."),
   NEED_ITEM_MATERIAL("Can not create item with a blank material value."),
   ITEM_MATERIAL_NULL("Failed to load item material like '%material%', possibly it doesn't exist or is spelled wrong."),
   ITEM_ENCHANTMENT_NULL("Could not get the item enchantment."),
   NEED_ITEM_ENCHANTMENT("Can not create enchantment with a blank value."),
   ITEM_ENCHANTMENT_NOT_SUPPORTED("This enchantment is not supported in this minecraft version."),
   ITEM_ENCHANTMENT_STRENGTH_NULL("Could not get the strength of the enchantment."),
   ENCHANTMENT_CANNOT_APPLY_TO_ITEM("To apply an enchantment to the item, the material needs to be enchanted book or a tool that fits the enchantment."),
   ITEM_SPAWNERTYPE_NULL("Could not get the spawner type of the item."),
   NEED_ITEM_SPAWNERTYPE("Can not create spawner with a empty spawner-type."),
   MATERIAL_NEEDS_TO_BE_SPAWNER("To apply a Spawner-Type(Entity-Type) to the item, the material of the item needs to be a spawner."),
   ITEM_POTIONTYPE_NULL("Could not get the potion type of the item."),
   NEED_ITEM_POTIONTYPE("Can not create a potiontype with a blank value."),
   POTIONTYPE_NOT_SUPPORTED("This potion type is not supported or doesn't exist."),
   MATERIAL_NEEDS_TO_BE_POTION("To apply an potion effect, the material needs to be a kind of a potion or a tipped arrow."),
   MATERIAL_NEEDS_TO_BE_LEATHER_ARMOR("To apply an color to a armor piece, the material needs to be leather armor."),
   CANT_BIND_MULTIPLE_APPLYMENTS("Can not create an item with multiple applyments like enchantments/spawner types or potion effect types."),
   ITEM_ERROR("&cItem Error, look in the console."),
   MATERIAL_NOT_SUPPORTED("This item material is not supported in this minecraft version."),
   ITEM_NEEDS_BUY_AND_SELL_VALUE("Every item in the shop needs to have a buy/sell value."),
   SELL_VALUE_WRONG("The sell value needs to be a decimal or a number."),
   BUY_VALUE_WRONG("The buy value needs to be a decimal or a number."),
   INVALID_BUY_PRICE("Invalid decimal format for '%price%' found for item buy price."),
   INVALID_SELL_PRICE("Invalid decimal format for '%price%' found for item sell price."),
   OPTION_TYPE_CHANGED(
      "Please change option 'type' to option 'spawnertype' to create a spawner in the shop. The name of the option has changed in version 2.2 for clarity."
   ),
   OPTION_META_CHANGED(
      "Please change option 'meta' to option 'displayname' to create a item with a displayname in the shop. The name of the option has changed in version 2.2 for clarity."
   ),
   CANNOT_DISPLAY_SHOPSECTION("Cannot create shop section '%shopsection%' because the place of the item needs to be in a range from 0 - 45."),
   NBTDATA_NEEDS_CURLY_BRACKETS("The NBT data specified, needs to start and end with a curly bracket."),
   NBTDATA_NULL("Could not get the NBT data."),
   CANNOT_GET_NBTDATA("Could not add the NBT data to the item, either the NBT data is spelled wrong or doesn't exist."),
   COMMAND_NULL("Could not get the command."),
   NEED_RECIPE_MATERIAL("Cannot create the recipe material with a blank value."),
   CANNOT_GET_RECIPE_MATERIAL("Cannot create the recipe material, please check the material name for any spelling mistakes and if it exists."),
   RECIPE_MATERIAL_NOT_SUPPORTED("The given recipe material is not supported in this minecraft version."),
   MATERIAL_NEEDS_TO_BE_BOOK("To apply an recipe to the item, the material of the item needs to be a KnowLedge book."),
   NEED_SKULLOWNER("Cannot create the PlayerHead without a specified player (Skull Owner)."),
   MATERIAL_NEEDS_TO_BE_SKULL("To apply an skull texture (Skull Owner) to the item, the material of the item needs to be a Player Head."),
   NOT_ENOUGH_STOCK_AVAILABLE("&cCannot buy the item(s) because there is not enough stock available."),
   MISSING_REQUIRED_ITEM_PERMISSIONS_RANK("&c&lThis item requires the &8&l%rank_name% &c&lrank"),
   MISSING_REQUIRED_ITEM_PERMISSIONS("&c&lYou do not have the required permissions for this item"),
   INVALID_HEADDATABASE_SKULL("Invalid head with ID of '%id%' inside HeadDatabase"),
   INVALID_ITEMSADDER_ITEM("Could not find item with ID of '%id%' inside ItemsAdder"),
   UNKNOWN_THIRD_PARTY_ITEM("Could not find item with ID of '%id%' inside %plugin%"),
   UNKNOWN_THIRD_PARTY_ENCHANTMENT("Could not find enchantment with ID of '%id%' inside %plugin%"),
   ERROR_CREATING_THIRD_PARTY_ITEM("Failed to create item with ID '%id%' from %plugin%"),
   HEADDATABASE_API_NOT_READY_YET("Failed to get HeadDatabase item, the HeadDatabase API isn't ready yet"),
   CANNOT_CREATE_ITEM_FROM_PLUGIN("Could not create item from %plugin%, the plugin was not found"),
   INVALID_ITEM_REQUIREMENT_TYPE("Invalid item requirement type for '%type%'"),
   INVALID_ITEM_REQUIREMENT("Invalid item requirement for %requirement%"),
   INVALID_SPAWN_POTENTIAL_WEIGHT("Invalid weight for spawn potential, should be a number"),
   INVALID_SPAWN_POTENTIAL("Invalid entry for spawn potential '%spawn_potential%', should be format of '<entityType>:<weight>'"),
   INVALID_MATERIAL_FOR_ITEM_OPTION("Material of item should be %material% to set %item-option%"),
   INVALID_OMINOUS_BOTTLE_STRENGTH("Invalid strength for ominous bottle, should be between 1 and 5, but got %strength%"),
   ERROR_SETTING_ITEM_OPTION("Failed to set the %item-option% for item with error:"),
   INVALID_ITEM_ACTION("Invalid item action for '%action%'"),
   CANNOT_DO_THAT_ACTION_ITEM("&cCannot do that for a action item"),
   ITEMS_PATH_IN_SHOPS_CONFIG("Item path in shops.yml: %location%"),
   ITEMS_PATH_IN_SECTIONS_CONFIG("Item path in sections.yml: %location%"),
   ITEMS_PATH_IN_CONFIG("Item path in config.yml: %location%"),
   ITEMS_LOCATION_IN_SHOPS("In shops config /shops/%section%.yml at location: %location%"),
   ITEMS_LOCATION_IN_SECTIONS("In sections config /sections/%section%.yml at location: %location%"),
   SPAWNER_COMPATIBILITY_ENABLED("Spawner provider set to %provider% in config"),
   SPAWNER_PROVIDER_FOUND("%provider% found, integrating..."),
   SPAWNER_PROVIDER_NOT_FOUND("%provider% was not found"),
   FAILED_SPAWNER_PROVIDER_INTEGRATION("Failed to integrate with %provider%"),
   USING_EXTERNAL_SPAWNERS("Using %provider% as external spawner provider..."),
   MINEABLESPAWNERS_COMPATIBILITY_ENABLED("Spawner provider set to MineableSpawners in config"),
   MINEABLESPAWNERS_FOUND("MineableSpawners found, integrating..."),
   MINEABLESPAWNERS_NOT_FOUND("MineableSpawners was not found"),
   COULD_NOT_INTEGRATE_WITH_MINEABLESPAWNERS("Could not integrate with MineableSpawners"),
   USING_MINEABLESPAWNERS_SPAWNERS("Using MineableSpawners as spawner provider..."),
   ROSESTACKER_COMPATIBILITY_ENABLED("Spawner provider set to RoseStacker in config"),
   ROSESTACKER_NOT_FOUND("RoseStacker was not found"),
   COULD_NOT_INTEGRATE_WITH_ROSESTACKER("Could not integrate with RoseStacker"),
   ROSESTACKER_FOUND("RoseStacker found, integrating..."),
   USING_ROSESTACKER_SPAWNERS("Using RoseStacker as spawner provider..."),
   EPICSPAWNERS_COMPATIBILITY_ENABLED("Spawner provider set to EpicSpawners in config"),
   EPICSPAWNERS_NOT_FOUND("EpicSpawners was not found"),
   COULD_NOT_INTEGRATE_WITH_EPICSPAWNERS("Could not integrate with EpicSpawners"),
   EPICSPAWNERS_FOUND("EpicSpawners found, integrating..."),
   USING_EPICSPAWNERS_SPAWNERS("Using EpicSpawners as spawner provider..."),
   ULTIMATESTACKER_COMPATIBILITY_ENABLED("Spawner provider set to UltimateStacker in config"),
   ULTIMATESTACKER_NOT_FOUND("UltimateStacker was not found"),
   COULD_NOT_INTEGRATE_WITH_ULTIMATESTACKER("Could not integrate with UltimateStacker"),
   ULTIMATESTACKER_FOUND("UltimateStacker found, integrating..."),
   USING_ULTIMATESTACKER_SPAWNERS("Using UltimateStacker as spawner provider..."),
   WILDSTACKER_COMPATIBILITY_ENABLED("Spawner provider set to WildStacker in config"),
   WILDSTACKER_NOT_FOUND("WildStacker was not found."),
   WILDSTACKER_FOUND("WildStacker found, integrating..."),
   COULD_NOT_INTEGRATE_WITH_WILDSTACKER("Could not integrate with WildStacker"),
   USING_WILDSTACKER_SPAWNERS("Using WildStacker as spawner provider."),
   SILKSPAWNERS_COMPATIBILITY_ENABLED("SilkSpawners compatibility has been enabled in config."),
   SILKSPAWNERS_NOT_FOUND("SilkSpawners not found, using OnyxShop spawners..."),
   SILKSPAWNERS_FOUND("SilkSpawners found, integrating..."),
   COULD_NOT_INTEGRATE_WITH_SILKSPAWNERS("Could not integrate with SilkSpawners."),
   USING_SILKSPAWNERS_SPAWNERS("Using SilkSpawners spawners..."),
   USING_DEFAULT_SPAWNERS("Using plugin default spawners..."),
   SPAWNER_PROVIDER_DISABLED("Spawner provider disabled in config, disabling..."),
   CANNOT_FIND_DISCOUNTED_SECTION("Cannot add discounts for section '%section%', either the shop section is disabled or not found."),
   CANNOT_FIND_DISCOUNTED_GROUP(
      "Cannot add discounts for permission group '%permsGroup%', please check the spelling and if it exists in your permissions plugin."
   ),
   DISABLING_PLUGIN("Disabling the plugin..."),
   NOT_SPAWN_ABLE("This entity type is not spawn able. '%spawnertype%'"),
   REAL_PLAYER("You need to be a real player to use that command."),
   PLAYER_NOT_ONLINE("&cThat player does not exist or is not online."),
   ITEM_NULL("&cThe item does not exist."),
   ITEM_CANNOT_BE_SOLD_TO_SERVER(
      "&cThe item (%material%) cannot be sold to the server because the item has a error inside the shop. Please report this issue to server admins or the owner."
   ),
   CANNOT_SELL_AIR("&cYou cannot sell air..."),
   NO_PERMISSIONS("&cYou do not have permissions for that."),
   NO_PERMISSIONS_TO_OPEN_SHOP("&cYou do not have the required permission to open this shop section."),
   CANNOT_DO_THAT("&4You can not do that!!"),
   MORE_SPACE_NEEDED("&cPlease make more space inside your inventory and then try again."),
   COULD_NOT_STORE_ALL_ITEMS("&c%amount% item(s) couldn't be added to your inventory because it is full."),
   NO_ITEM_FOUND("&cNo items found which can be sold."),
   NOT_ENOUGH_ITEMS_TO_SELL("&cIt is required to sell atleast %min-sell%, you only have %quantity%"),
   NOT_ENOUGH_ITEMS_TO_BUY("&cIt is required to buy atleast %min-buy%"),
   NOT_ENOUGH_SPACE_INSIDE_INVENTORY("&4You do not have enough space in your inventory, dropping the remaining items on ground."),
   CANCELING_TRANSACTION_NO_SPACE(
      "&4You do not have enough space in your inventory, canceling transaction. \nPlease make more room inside your inventory and try again."
   ),
   INSUFFICIENT_MONEY("&cYou do not have enough %currency%."),
   NO_ITEMS_IN_SECTION("Can not open shop section %shopsection% because there are no items found!"),
   COULD_NOT_SAVE_CONFIG("Could not save %fileName% config."),
   RGB_COLOR_FORMATTED_WRONG("RGB colors must be formatted as: '0x000000', '#000000' or '000000' at %path%."),
   NO_SHOP_FOUND("&cCan not find a shop section called by that name"),
   NO_VALID_AMOUNT("&cNot a valid amount"),
   NO_SHOP_PAGE_FOUND("&cNo shop page found for %page%"),
   RESTART_PLUGIN("Can not open the shop section, try restarting the plugin or join the discord server for support."),
   COULD_NOT_CHECK_FOR_UPDATES("Could not check for updates."),
   UPDATE_DISABLED("&cUpdate checking is disabled on this server."),
   CLOUD_DISABLED("&cCloud features are disabled on this server."),
   ERROR_OCCURRED_WHILE_RELOADING("&4A error has occurred while reloading the plugin, please look inside the console for any error message."),
   COULD_NOT_GET_THE_LIMITEDSTOCK_MODE("Could not get the limitedstock mode: %limitedStockMode%"),
   COULD_NOT_ENABLE_LIMITEDSTOCK("Could not enable the limited item stock provider"),
   COULD_NOT_ENABLE_LIMITEDSELLSTOCK("Could not enable the limited sell stock provider"),
   COULD_NOT_ENABLE_DYNAMICPRICING("Could not enable the dynamic pricing provider"),
   ERROR_OCCURRED_WHILE_CONNECTING_TO_DATABASE("A error occurred while trying to connect to the database"),
   INVALID_STORAGE_TYPE("Invalid storage type for '%type%'. Using SQLite as default..."),
   INVALID_ITEM_TYPE("&cInvalid item type for %type%"),
   CANNOT_ENTER_SHOP_BANNED_GAMEMODE("&cYou cannot buy/sell items while being in %gamemode%."),
   CANNOT_USE_COMMAND_BANNED_GAMEMODE("&cYou cannot use this command while being in %gamemode%."),
   CANNOT_STORE_PAYMENT("&cUnable to complete transaction, cannot store all payment items inside your inventory. Make more free space and try again."),
   INVALID_BUY_PRICE_SCALING("The buy price scaling for DynamicPricing must be between 0-100%, got %scale%. Using default of %default%%..."),
   INVALID_SELL_PRICE_SCALING("The sell price scaling for DynamicPricing must be between 0-100%, got %scale%. Using default of %default%%..."),
   EXCEPTION_LOADING_FILL_ITEM("A exception occurred while loading shop fill-Item for section '%section%'."),
   CANNOT_LOAD_SEASON_MODIFIER("Failed to load season modifier for '%path%' because there was no loaded section/item found."),
   CANNOT_LOAD_SEASON_MODIFIER_ACTION_ITEM("Cannot load season modifier for '%path%' as the shop item contains a action"),
   INVALID_SEASON_MODIFIER("Invalid modifier found at '%path%', it needs to be a valid percent between -100 and 100 like '-75%', '+25%', ... Got %current%"),
   CANNOT_LOAD_ECONOMY_PROVIDER("Failed to load economy provider for shop '%shop%' with reason '%reason%', using default..."),
   CANNOT_FIND_ECONOMY_PROVIDER("Could not find a economy provider for shop '%shop%' such as '%type%', using default..."),
   CANNOT_FIND_DEFAULT_ECONOMY_PROVIDER(
      "Could not find the economy type specified inside the config.yml such as '%economy%', trying to automatically find supported economy type..."
   ),
   CANNOT_LOAD_DEFAULT_ECONOMY_PROVIDER(
      "Failed to load default economy provider with reason '%reason%', trying to automatically find supported economy provider"
   ),
   CANNOT_FIND_ITEM_ECONOMY_PROVIDER("Could not find a economy provider for shop item at '%path%' such as '%type%'..."),
   CANNOT_LOAD_ITEM_ECONOMY_PROVIDER("Failed to load economy provider for shop item '%path%' such as '%type%' with reason '%reason%'..."),
   TRANSACTION_PLACEHOLDER_DEFAULT("No data yet for this time frame"),
   REMOVED_SHOP_STAND("&cRemoved shop stand from location %location%"),
   SHOP_STAND_LOAD_FAILED("Failed to load shop stand with ID '%id%': %reason%"),
   SHOP_STAND_UNLOAD_FAILED("Failed to unload shop stand with ID '%id%': %reason%"),
   SHOP_STAND_INSIDE_UNLOADED_CHUNK("Shop stand is inside a unloaded chunk"),
   ITEM_FROM_SHOP_STAND_NOT_FOUND("Cannot find shop item from shop stand '%item%'"),
   BLOCK_FROM_SHOP_STAND_NOT_FOUND(
      "Block type from stand at location %location% does not match with cached block type of '%type%'. Possibly removed from world manually but not from plugin?"
   ),
   INVALID_SHOP_STAND_WORLD("Invalid stand location for world '%world%', does not exist for shop stand ID %id%"),
   INVALID_SHOP_STAND_FORMAT("Invalid stand location with x%pos_x%, y%pos_y%, z%pos_z% for shop stand ID %id%"),
   ERROR_SAVING_SHOP_STANDS("Failed to save shop stands to 'stands.json'"),
   ERROR_LOADING_SHOP_STANDS("Failed to load shop stands from 'stands.json'"),
   ERROR_LOADING_SHOP_STAND("Failed to load shop stand('%stand%') from stands.json with reason: %reason%"),
   ERROR_CREATING_STANDS_STORAGE("Failed to create 'stands.json' storage file"),
   INVALID_STAND_TYPE("Invalid shop stand type for %type%"),
   SHOP_STANDS_MODULE_NOT_ENABLED("Shop stands module not enabled"),
   CANNOT_OPEN_SHOP_STAND("Failed to open shop stand for player %player%: ShopItem(%item%) does not exist!"),
   INVALID_CLICK_TYPE("Invalid click type for %click-type% with entry %click-mapping%, using default..."),
   INVALID_CLICK_ACTION("Invalid click action for %click-action% with entry %click-mapping%, using default..."),
   ERROR_SAVING_PRICE_MODIFIERS("Failed to save modifiers to database: %message%");

   private String def;
   private static final OnyxShop plugin = OnyxShop.getInstance();
   private static final boolean supportsComponents = !(OnyxShop.getInstance().getMetaUtils() instanceof BukkitMeta);
   private static Locale defaultLang;
   private static boolean perPlayerLanguages;
   private static final Map<Locale, Lang.LangCache> langCache = new HashMap<>();

   private Lang(String nullxx) {
      this.def = nullxx;
   }

   public void reload() {
      if (!langCache.isEmpty()) {
         langCache.clear();
      }

      plugin.startupReload.checkIfLanguageFilesExist();
      boolean var1 = true;
      String var2 = ConfigManager.badYaml != null ? "lang-en.yml" : ConfigManager.getConfig().getString("language-file", "lang-en.yml");

      try {
         defaultLang = Locale.forLanguageTag(var2.split("-")[1].split("\\.")[0].replace("_", "-"));
      } catch (ArrayIndexOutOfBoundsException var14) {
         var1 = false;
      }

      if (!var1 || defaultLang.getLanguage().isEmpty()) {
         SendMessage.errorMessage("Failed to get language for file name '%fileName%', using English as default...".replace("%fileName%", var2));
         defaultLang = Locale.ENGLISH;
      }

      try {
         langCache.put(defaultLang, new Lang.LangCache(var2));
         SendMessage.infoMessage("Using " + var2 + " as language file");
      } catch (ConfigLoadException var13) {
         SendMessage.errorMessage("Failed to load language file '%fileName%' from config.yml, using English as default...".replace("%fileName%", var2));

         try {
            langCache.put(defaultLang = Locale.ENGLISH, new Lang.LangCache("lang-en.yml"));
         } catch (ConfigLoadException var12) {
            SendMessage.errorMessage("Failed to load default lang-en.yml, this is a bug!");
         }
      }

      if (ConfigManager.badYaml == null && ConfigManager.getConfig().getBoolean("per-player-languages", true)) {
         SendMessage.infoMessage(LOADING_LANGUAGE_FILES.get());
         File var3 = new File(plugin.getDataFolder() + File.separator + "LanguageFiles");
         if (var3.exists() && var3.isDirectory()) {
            perPlayerLanguages = true;

            for (File var7 : var3.listFiles()) {
               if (!var7.getName().equals(var2)) {
                  Locale var8 = null;

                  try {
                     var8 = Locale.forLanguageTag(var7.getName().split("-")[1].split("\\.")[0].replace("_", "-"));
                  } catch (ArrayIndexOutOfBoundsException var11) {
                     var1 = false;
                  }

                  if (var1 && var8 != null && !var8.getLanguage().isEmpty()) {
                     try {
                        langCache.put(var8, new Lang.LangCache(var7));
                     } catch (ConfigLoadException var10) {
                        SendMessage.errorMessage(FAILED_TO_LOAD_LANGUAGE_FILE.get().replace("%fileName%", var7.getName()));
                     }
                  } else {
                     SendMessage.errorMessage(FAILED_TO_GET_LOCALE.get().replace("%fileName%", var7.getName()));
                  }
               }
            }

            SendMessage.infoMessage(LOADED_LANGUAGE_FILES.get().replace("%amount%", String.valueOf(langCache.size())));
         } else {
            SendMessage.errorMessage(FAILED_TO_LOAD_LANGUAGE_FILES.get());
         }
      }
   }

   public String getKey() {
      return this.name().toLowerCase(Locale.ENGLISH).replace("_", "-").replace("0", ",");
   }

   public void clearMessages() {
      if (!langCache.isEmpty()) {
         langCache.clear();
      }
   }

   public Translatable get(Player var1) {
      return perPlayerLanguages ? this.get(UserManager.getUser(var1).getLocale()) : this.get();
   }

   public Translatable get(Locale var1) {
      return langCache.getOrDefault(var1, langCache.get(defaultLang)).get(this.getKey(), this.def);
   }

   public Translatable get() {
      return langCache.get(defaultLang).get(this.getKey(), this.def);
   }

   public static Translatable getOrNull(String var0) {
      return langCache.get(defaultLang).getOrNull(var0);
   }

   public static Translatable getOrNull(String var0, Locale var1) {
      return langCache.getOrDefault(var1, langCache.get(defaultLang)).getOrNull(var0);
   }

   public static Set<Locale> getLocales() {
      return langCache.keySet();
   }

   public static Locale getDefaultLocale() {
      return defaultLang;
   }

   public List<Translatable> getMultiple() {
      return supportsComponents ? Arrays.stream(this.getRaw().split("\\\\n|\\n")).map(Lang::translate).collect(Collectors.toList()) : this.get().getLines();
   }

   public String getRaw() {
      return langCache.get(defaultLang).getRaw(this.getKey(), this.def);
   }

   public String getRaw(Locale var1) {
      return langCache.getOrDefault(var1, langCache.get(defaultLang)).getRaw(this.getKey(), this.def);
   }

   private static Translatable translate(String var0) {
      return (Translatable)(!supportsComponents
         ? new TranslatableRaw(var0)
         : (Translatable)ReflectUtil.getPaperFeature("files.lang.TranslatableComponent", var0));
   }

   private static FileConfiguration getLangFile(File var0) {
      if (!var0.exists()) {
         return null;
      } else {
         YamlConfiguration var1 = plugin.loadConfiguration(var0, var0.getName());
         if (var1 == null) {
            return null;
         } else {
            try {
               plugin.saveResource("LanguageFiles" + File.separator + var0.getName(), true);
               YamlConfiguration var2 = plugin.loadConfiguration(var0, var0.getName());
               if (var2 == null) {
                  ConfigUtil.save(var1, var0);
                  return var1;
               } else {
                  for (String var4 : var1.getKeys(false)) {
                     var2.set(var4, var1.get(var4));
                  }

                  ConfigUtil.save(var2, var0);
                  return var2;
               }
            } catch (Exception var5) {
               SendMessage.logDebugMessage("Failed to automatically update the language file for " + var0.getName());
               return plugin.loadConfiguration(var0, var0.getName());
            }
         }
      }
   }

   private static final class LangCache {
      private final FileConfiguration config;
      private final HashMap<String, Translatable> cache = new HashMap<>();

      public LangCache(String var1) throws ConfigLoadException {
         this.config = Lang.getLangFile(new File(Lang.plugin.getDataFolder() + File.separator + "LanguageFiles", var1));
         if (this.config == null) {
            throw new ConfigLoadException("Can not find or load language file '" + var1 + "'");
         }
      }

      public LangCache(File var1) throws ConfigLoadException {
         this.config = Lang.getLangFile(var1);
         if (this.config == null) {
            throw new ConfigLoadException("Can not find or load language file '" + var1.getName() + "'");
         }
      }

      public Translatable get(String var1, String var2) {
         if (this.cache.containsKey(var1)) {
            return this.cache.get(var1);
         } else {
            Translatable var3;
            try {
               var3 = Lang.translate(this.config.getString(var1, var2));
            } catch (NullPointerException var5) {
               var3 = Lang.translate(var2);
            }

            this.cache.put(var1, var3);
            return var3;
         }
      }

      public Translatable getOrNull(String var1) {
         if (this.cache.containsKey(var1)) {
            return this.cache.get(var1);
         } else {
            String var3 = this.config.getString(var1);
            if (var3 == null) {
               return null;
            } else {
               Translatable var2 = Lang.translate(var3);
               this.cache.put(var1, var2);
               return var2;
            }
         }
      }

      public String getRaw(String var1, String var2) {
         try {
            return this.config.getString(var1, var2);
         } catch (NullPointerException var4) {
            return var2;
         }
      }
   }
}
