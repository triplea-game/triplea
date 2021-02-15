package org.triplea.java;

import com.google.common.base.Charsets;
import java.net.URLDecoder;
import lombok.experimental.UtilityClass;

/** Utility methods for working with URLs and URL-formatted String objects. */
@UtilityClass
public class UrlUtils {

  /** URL decodes a given string, eg: replaces '%20' with a space. */
  public String urlDecode(final String urlEncoded) {
    return URLDecoder.decode(urlEncoded, Charsets.UTF_8);
  }
}
