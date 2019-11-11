package org.triplea.test.common;

import com.google.common.base.Strings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

@UtilityClass
public class StringToInputStream {

  /** Converts a {@code String} to an equivalent {@code InputStream}. */
  public static InputStream asInputStream(final String inputString) {
    return IOUtils.toInputStream(Strings.nullToEmpty(inputString), StandardCharsets.UTF_8);
  }
}
