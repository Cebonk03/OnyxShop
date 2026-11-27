package com.onyxshop.util.exceptions;

public class CustomException extends Exception {
   public CustomException(String text) {
      super(text);
   }

   @Override
   public String toString() {
      return this.getMessage();
   }
}
