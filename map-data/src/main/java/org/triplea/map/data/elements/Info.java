package org.triplea.map.data.elements;

import lombok.Getter;
import org.triplea.map.reader.Attribute;
import org.triplea.map.reader.TagName;

@Getter
@TagName("info")
public class Info {
  public static final String TAG_NAME = "info";
  @Attribute private String name;
  @Attribute private String version;
}
