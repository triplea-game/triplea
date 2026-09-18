package org.triplea.java;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlUtilsTest {
  @Test
  void urlDecode() {
    assertThat(UrlUtils.urlDecode("")).isEqualTo("");
    assertThat(UrlUtils.urlDecode("abc")).isEqualTo("abc");
    assertThat(UrlUtils.urlDecode(" ")).isEqualTo(" ");
    assertThat(UrlUtils.urlDecode("%20")).isEqualTo(" ");
  }
}
