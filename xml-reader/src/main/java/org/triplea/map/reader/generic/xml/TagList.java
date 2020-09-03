package org.triplea.map.reader.generic.xml;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TagList {
  Class<?> value();
}
