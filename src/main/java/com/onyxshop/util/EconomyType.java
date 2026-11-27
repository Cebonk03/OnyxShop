package com.onyxshop.util;

import com.onyxshop.methodes.SendMessage;

public enum EconomyType {
   EXP("EXP"),
   ITEM("Item"),
   VAULT("Vault"),
   VAULTUNLOCKED("VaultUnlocked"),
   LEVELS("Levels"),
   ECOBITS("EcoBits"),
   EXCELLENT_ECONOMY("Excellent Economy"),
   PLAYER_POINTS("PlayerPoints"),
   GEMS_ECONOMY("GemsEconomy"),
   ULTRA_ECONOMY("UltraEconomy"),
   VOTING_PLUGIN("VotingPlugin"),
   ZESSENTIALS("zEssentials"),
   EXTERNAL("EXTERNAL");

   final String name;

   private EconomyType(String type) {
      this.name = type;
   }

   public String getName() {
      return this.name;
   }

   public static EcoType getFromString(String type) {
      if (type == null) {
         return null;
      } else {
         String text = type.split(":")[0].toUpperCase();
         EcoType ecoType;
         switch (text) {
            case "EXP":
               ecoType = new EcoType(EXP);
               break;
            case "ITEM":
               try {
                  ecoType = new EcoType(ITEM, type.replaceFirst("ITEM:", ""));
               } catch (ArrayIndexOutOfBoundsException exception) {
                  SendMessage.warnMessage(
                     "Invalid format for item economy '" + type + "', should be 'ITEM:material:<item>'. Using default item: GOLD_NUGGET..."
                  );
                  ecoType = new EcoType(ITEM, "GOLD_INGOT");
               }
               break;
            case "PLAYER_POINTS":
               ecoType = new EcoType(PLAYER_POINTS);
               break;
            case "LEVELS":
               ecoType = new EcoType(LEVELS);
               break;
            case "VAULT":
               ecoType = new EcoType(VAULT);
               break;
            case "VAULTUNLOCKED":
               if (type.contains(":")) {
                  ecoType = new EcoType(VAULTUNLOCKED, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(VAULTUNLOCKED);
               }
               break;
            case "ECOBITS":
               if (type.contains(":")) {
                  ecoType = new EcoType(ECOBITS, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(ECOBITS);
               }
               break;
            case "EXCELLENT_ECONOMY":
               if (type.contains(":")) {
                  ecoType = new EcoType(EXCELLENT_ECONOMY, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(EXCELLENT_ECONOMY);
               }
               break;
            case "GEMS_ECONOMY":
               if (type.contains(":")) {
                  ecoType = new EcoType(GEMS_ECONOMY, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(GEMS_ECONOMY);
               }
               break;
            case "ULTRA_ECONOMY":
               if (type.contains(":")) {
                  ecoType = new EcoType(ULTRA_ECONOMY, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(ULTRA_ECONOMY);
               }
               break;
            case "VOTING_PLUGIN":
               ecoType = new EcoType(VOTING_PLUGIN);
               break;
            case "ZESSENTIALS":
               if (type.contains(":")) {
                  ecoType = new EcoType(ZESSENTIALS, type.split(":")[1]);
               } else {
                  ecoType = new EcoType(ZESSENTIALS);
               }
               break;
            case "EXTERNAL":
               if (type.contains(":")) {
                  ecoType = new EcoType(EXTERNAL, type.split(":", 2)[1]);
               } else {
                  ecoType = null;
               }
               break;
            default:
               ecoType = null;
         }

         return ecoType;
      }
   }
}
