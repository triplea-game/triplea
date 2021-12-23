package org.triplea.test.common;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;

@UtilityClass
public class StringToInputStream {

  /** Converts a {@code String} to an equivalent {@code InputStream}. */
  public static InputStream asInputStream(final String inputString) {
    return new ByteArrayInputStream(
        Strings.nullToEmpty(inputString).getBytes(StandardCharsets.UTF_8));
  }
}
