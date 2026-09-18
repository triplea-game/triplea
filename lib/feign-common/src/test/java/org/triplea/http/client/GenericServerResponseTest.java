package org.triplea.http.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GenericServerResponseTest {

  @Test
  void builder() {
    GenericServerResponse genericServerResponse = GenericServerResponse.builder().build();
    assertThat(genericServerResponse.isSuccess()).isFalse();
    assertThat(genericServerResponse.getMessage()).isNull();
  }

  @Test
  void isSuccess() {
    GenericServerResponse genericServerResponse =
        GenericServerResponse.builder().success(true).build();
    assertThat(genericServerResponse.isSuccess()).isTrue();
    assertThat(GenericServerResponse.SUCCESS).isEqualTo(genericServerResponse);
  }

  @Test
  void getMessage() {
    GenericServerResponse genericServerResponse =
        GenericServerResponse.builder().message("test").build();
    assertThat(genericServerResponse.getMessage()).isEqualTo("test");
  }
}
