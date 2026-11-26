package com.onyxshop.util;

import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public final class JsonUtil {
   private static final JSONParser parser = new JSONParser();

   public JsonUtil() {
   }

   public static JSONObject parseJson(InputStreamReader var0) throws ParseException, IOException {
      return (JSONObject)parser.parse(var0);
   }

   public static JSONObject parseJson(FileReader var0) throws ParseException, IOException {
      return (JSONObject)parser.parse(var0);
   }

   public static JSONObject parseJson(String var0) throws ParseException {
      return (JSONObject)parser.parse(var0);
   }

   public static JSONArray parseArray(List<String> var0) throws ParseException {
      return (JSONArray)parser.parse(var0.toString());
   }

   public static JSONArray parseArray(String var0) throws ParseException {
      return (JSONArray)parser.parse(var0);
   }

   public static JSONArray parseArray(InputStreamReader var0) throws ParseException, IOException {
      return (JSONArray)parser.parse(var0);
   }

   public static String getString(JSONObject var0, String var1) {
      return (String)var0.get(var1);
   }

   public static Long getLong(JSONObject var0, String var1) {
      return (Long)var0.get(var1);
   }

   public static ArrayList<String> getArray(JSONObject var0, String var1) {
      return (ArrayList<String>)var0.get(var1);
   }

   public static Map<String, String> getMap(JSONObject var0, String var1) {
      return (Map<String, String>)var0.get(var1);
   }
}
