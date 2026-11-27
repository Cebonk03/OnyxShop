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

   public static JSONObject parseJson(InputStreamReader inputStreamReader) throws ParseException, IOException {
      return (JSONObject)parser.parse(inputStreamReader);
   }

   public static JSONObject parseJson(FileReader fileReader) throws ParseException, IOException {
      return (JSONObject)parser.parse(fileReader);
   }

   public static JSONObject parseJson(String text) throws ParseException {
      return (JSONObject)parser.parse(text);
   }

   public static JSONArray parseArray(List<String> values) throws ParseException {
      return (JSONArray)parser.parse(values.toString());
   }

   public static JSONArray parseArray(String text) throws ParseException {
      return (JSONArray)parser.parse(text);
   }

   public static JSONArray parseArray(InputStreamReader inputStreamReader) throws ParseException, IOException {
      return (JSONArray)parser.parse(inputStreamReader);
   }

   public static String getString(JSONObject jsonObject, String text) {
      return (String)jsonObject.get(text);
   }

   public static Long getLong(JSONObject jsonObject, String text) {
      return (Long)jsonObject.get(text);
   }

   public static ArrayList<String> getArray(JSONObject jsonObject, String text) {
      return (ArrayList<String>)jsonObject.get(text);
   }

   public static Map<String, String> getMap(JSONObject jsonObject, String text) {
      return (Map<String, String>)jsonObject.get(text);
   }
}
