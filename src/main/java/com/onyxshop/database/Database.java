package com.onyxshop.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.onyxshop.database.queries.Query;
import com.onyxshop.objects.PlayerPriceModifier;
import com.onyxshop.providers.ScheduleManager;
import com.onyxshop.util.SimplePair;

public interface Database {
   boolean connect();

   void closeConnection();

   PlayerPriceModifier loadPlayerPriceModifiers(UUID var1) throws SQLException;

   void savePlayerPriceModifiers(UUID var1, PlayerPriceModifier var2) throws SQLException;

   int removeExpiredPlayerData(long var1) throws SQLException;

   Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException;

   void removeExpiredItemData(Map<String, Map<String, Set<String>>> var1) throws SQLException;

   int getOriginalStock(String var1);

   void setOriginalStock(String var1, int var2);

   void setItemStock(String var1, int var2);

   void setItemStock(Map<String, Integer> var1) throws SQLException;

   void setItemRestockTimes(Map<String, Long> var1) throws SQLException;

   int getItemStock(String var1) throws SQLException;

   Map<String, Long> getItemRestockTime() throws SQLException;

   void setPlayerStockAmount(UUID var1, String var2, Integer var3);

   void setPlayerStockAmount(String var1, Integer var2);

   void setPlayerStock(Map<String, Map<UUID, Integer>> var1) throws SQLException;

   void setPlayerRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException;

   int getPlayerStockAmount(UUID var1, String var2) throws SQLException;

   Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException;

   Map<String, Integer> getPlayersStock(String var1) throws SQLException;

   void setItemStockSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException;

   Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException;

   Map<String, Integer> getOriginalStocks() throws SQLException;

   Map<String, Integer> getItemStocks(Collection<String> var1) throws SQLException;

   Map<String, Integer> getPlayerStockAmounts(Collection<String> var1, UUID var2) throws SQLException;

   Map<String, Integer> getAllItemStocks() throws SQLException;

   Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException;

   Map<String, Integer> loadDynamicPricingStock() throws SQLException;

   Integer getDynamicStock(String var1);

   Map<String, Integer> loadDynamicMaxStocks() throws SQLException;

   void setDynamicMaxStock(String var1, int var2);

   void setDynamicStock(String var1, int var2);

   void saveDynamicStock(Map<String, Integer> var1);

   boolean resetDynamicPricing();

   Map<String, Integer> getDynamicStocks(Collection<String> var1) throws SQLException;

   int getOriginalLimit(String var1);

   void setOriginalLimit(String var1, int var2);

   void setSellLimit(String var1, int var2);

   void setSellLimit(Map<String, Integer> var1) throws SQLException;

   void setSellLimitRestockTimes(Map<String, Long> var1) throws SQLException;

   int getSellLimit(String var1) throws SQLException;

   Map<String, Long> getSellLimitRestockTimes() throws SQLException;

   void setPlayerSellLimit(UUID var1, String var2, Integer var3);

   void setPlayerSellLimit(String var1, Integer var2);

   void setPlayerSellLimit(Map<String, Map<UUID, Integer>> var1) throws SQLException;

   void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> var1) throws SQLException;

   int getPlayerSellLimit(UUID var1, String var2) throws SQLException;

   Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException;

   Map<String, Integer> getPlayerSellLimits(String var1) throws SQLException;

   void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> var1) throws SQLException;

   Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException;

   Map<String, Integer> getOriginalLimits() throws SQLException;

   Map<String, Integer> getSellLimits(Collection<String> var1) throws SQLException;

   Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> var1, UUID var2) throws SQLException;

   Map<String, Integer> getAllSellLimits() throws SQLException;

   Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException;

   void executeBatch(SQLBatch.Type var1, Set<Query> var2);

   void executeQuery(SQLBatch.Type var1, Query... var2);

   void setRotatingShopItems(String var1, List<String> var2, long var3);

   SimplePair<List<String>, Long> getRotatingShopItems(String var1) throws SQLException;
}
