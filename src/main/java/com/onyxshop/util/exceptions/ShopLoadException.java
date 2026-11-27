package com.onyxshop.util.exceptions;

public class ShopLoadException extends CustomException {
   public ShopLoadException(String shopId) {
      super(shopId);
   }
}
