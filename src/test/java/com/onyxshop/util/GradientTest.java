package com.onyxshop.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GradientTest {
   @Test
   void visibleCharactersSurviveAndHexTokensAreRemoved() {
      String rendered = new Gradient("#FF0000", "#00FF00", "#FF0000ab#00FF00").get();

      assertTrue(rendered.contains("a"), rendered);
      assertTrue(rendered.contains("b"), rendered);
      assertFalse(rendered.contains("#FF0000"), rendered);
      assertFalse(rendered.contains("#00FF00"), rendered);
   }
}
