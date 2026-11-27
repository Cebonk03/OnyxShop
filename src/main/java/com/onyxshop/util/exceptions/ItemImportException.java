package com.onyxshop.util.exceptions;

public class ItemImportException extends CustomException {
   public ItemImportException(String itemId) {
      super(itemId);
   }
}
