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

   PlayerPriceModifier loadPlayerPriceModifiers(UUID playerId) throws SQLException;

   void savePlayerPriceModifiers(UUID playerId, PlayerPriceModifier playerPriceModifier) throws SQLException;

   int removeExpiredPlayerData(long value) throws SQLException;

   Map<String, Map<String, Set<String>>> getExpiredItemData() throws SQLException;

   void removeExpiredItemData(Map<String, Map<String, Set<String>>> map) throws SQLException;

   int getOriginalStock(String itemId);

   void setOriginalStock(String itemId, int value);

   void setItemStock(String itemId, int stock);

   void setItemStock(Map<String, Integer> stocks) throws SQLException;

   void setItemRestockTimes(Map<String, Long> restockTimes) throws SQLException;

   int getItemStock(String itemId) throws SQLException;

   Map<String, Long> getItemRestockTime() throws SQLException;

   void setPlayerStockAmount(UUID playerId, String itemId, Integer amount);

   void setPlayerStockAmount(String playerName, Integer stock);

   void setPlayerStock(Map<String, Map<UUID, Integer>> map) throws SQLException;

   void setPlayerRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException;

   int getPlayerStockAmount(UUID playerId, String itemId) throws SQLException;

   Map<String, Map<UUID, Long>> getPlayerRestockTimes() throws SQLException;

   Map<String, Integer> getPlayersStock(String itemId) throws SQLException;

   void setItemStockSchedulers(Map<ScheduleManager.Item, Long> stocks) throws SQLException;

   Map<ScheduleManager.Item, Long> getItemStockSchedulers() throws SQLException;

   Map<String, Integer> getOriginalStocks() throws SQLException;

   Map<String, Integer> getItemStocks(Collection<String> itemIds) throws SQLException;

   Map<String, Integer> getPlayerStockAmounts(Collection<String> playerNames, UUID uniqueId) throws SQLException;

   Map<String, Integer> getAllItemStocks() throws SQLException;

   Map<String, Map<UUID, Integer>> getAllPlayerStocks() throws SQLException;

   Map<String, Integer> loadDynamicPricingStock() throws SQLException;

   Integer getDynamicStock(String itemId);

   Map<String, Integer> loadDynamicMaxStocks() throws SQLException;

   void setDynamicMaxStock(String itemId, int value);

   void setDynamicStock(String itemId, int value);

   void saveDynamicStock(Map<String, Integer> stocks);

   boolean resetDynamicPricing();

   Map<String, Integer> getDynamicStocks(Collection<String> values) throws SQLException;

   int getOriginalLimit(String text);

   void setOriginalLimit(String text, int limit);

   void setSellLimit(String text, int limit);

   void setSellLimit(Map<String, Integer> limits) throws SQLException;

   void setSellLimitRestockTimes(Map<String, Long> limits) throws SQLException;

   int getSellLimit(String text) throws SQLException;

   Map<String, Long> getSellLimitRestockTimes() throws SQLException;

   void setPlayerSellLimit(UUID playerId, String text, Integer limit);

   void setPlayerSellLimit(String playerName, Integer limit);

   void setPlayerSellLimit(Map<String, Map<UUID, Integer>> map) throws SQLException;

   void setPlayerSellLimitRestockTimes(Map<String, Map<UUID, Long>> map) throws SQLException;

   int getPlayerSellLimit(UUID playerId, String text) throws SQLException;

   Map<String, Map<UUID, Long>> getPlayerSellLimitRestockTimes() throws SQLException;

   Map<String, Integer> getPlayerSellLimits(String playerName) throws SQLException;

   void setSellLimitSchedulers(Map<ScheduleManager.Item, Long> limits) throws SQLException;

   Map<ScheduleManager.Item, Long> getSellLimitSchedulers() throws SQLException;

   Map<String, Integer> getOriginalLimits() throws SQLException;

   Map<String, Integer> getSellLimits(Collection<String> values) throws SQLException;

   Map<String, Integer> getPlayerSellLimitAmounts(Collection<String> playerNames, UUID uniqueId) throws SQLException;

   Map<String, Integer> getAllSellLimits() throws SQLException;

   Map<String, Map<UUID, Integer>> getAllPlayerSellLimits() throws SQLException;

   void executeBatch(SQLBatch.Type type, Set<Query> values);

   void executeQuery(SQLBatch.Type type, Query... types);

   void setRotatingShopItems(String shopId, List<String> values, long value);

   SimplePair<List<String>, Long> getRotatingShopItems(String shopId) throws SQLException;
}
