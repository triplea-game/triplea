package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class UrlUtilsTest {
  @Test
  void urlDecode() {
    assertThat(UrlUtils.urlDecode(""), is(""));
    assertThat(UrlUtils.urlDecode("abc"), is("abc"));
    assertThat(UrlUtils.urlDecode(" "), is(" "));
    assertThat(UrlUtils.urlDecode("%20"), is(" "));
  }
}
