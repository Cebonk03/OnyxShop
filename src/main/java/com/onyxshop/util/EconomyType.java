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

   private EconomyType(String nullxx) {
      this.name = nullxx;
   }

   public String getName() {
      return this.name;
   }

   public static EcoType getFromString(String var0) {
      if (var0 == null) {
         return null;
      } else {
         String var2 = var0.split(":")[0].toUpperCase();
         EcoType var1;
         switch (var2) {
            case "EXP":
               var1 = new EcoType(EXP);
               break;
            case "ITEM":
               try {
                  var1 = new EcoType(ITEM, var0.replaceFirst("ITEM:", ""));
               } catch (ArrayIndexOutOfBoundsException var5) {
                  SendMessage.warnMessage(
                     "Invalid format for item economy '" + var0 + "', should be 'ITEM:material:<item>'. Using default item: GOLD_NUGGET..."
                  );
                  var1 = new EcoType(ITEM, "GOLD_INGOT");
               }
               break;
            case "PLAYER_POINTS":
               var1 = new EcoType(PLAYER_POINTS);
               break;
            case "LEVELS":
               var1 = new EcoType(LEVELS);
               break;
            case "VAULT":
               var1 = new EcoType(VAULT);
               break;
            case "VAULTUNLOCKED":
               if (var0.contains(":")) {
                  var1 = new EcoType(VAULTUNLOCKED, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(VAULTUNLOCKED);
               }
               break;
            case "ECOBITS":
               if (var0.contains(":")) {
                  var1 = new EcoType(ECOBITS, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(ECOBITS);
               }
               break;
            case "EXCELLENT_ECONOMY":
               if (var0.contains(":")) {
                  var1 = new EcoType(EXCELLENT_ECONOMY, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(EXCELLENT_ECONOMY);
               }
               break;
            case "GEMS_ECONOMY":
               if (var0.contains(":")) {
                  var1 = new EcoType(GEMS_ECONOMY, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(GEMS_ECONOMY);
               }
               break;
            case "ULTRA_ECONOMY":
               if (var0.contains(":")) {
                  var1 = new EcoType(ULTRA_ECONOMY, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(ULTRA_ECONOMY);
               }
               break;
            case "VOTING_PLUGIN":
               var1 = new EcoType(VOTING_PLUGIN);
               break;
            case "ZESSENTIALS":
               if (var0.contains(":")) {
                  var1 = new EcoType(ZESSENTIALS, var0.split(":")[1]);
               } else {
                  var1 = new EcoType(ZESSENTIALS);
               }
               break;
            case "EXTERNAL":
               if (var0.contains(":")) {
                  var1 = new EcoType(EXTERNAL, var0.split(":", 2)[1]);
               } else {
                  var1 = null;
               }
               break;
            default:
               var1 = null;
         }

         return var1;
      }
   }
}
