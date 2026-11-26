package org.quartz;

import java.io.Serializable;
import org.quartz.utils.Key;

public interface Matcher<T extends Key<?>> extends Serializable {
   boolean isMatch(T var1);

   @Override
   int hashCode();

   @Override
   boolean equals(Object var1);
}
