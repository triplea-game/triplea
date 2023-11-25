package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class GenericServerResponseTest {

  @Test
  void builder() {
    GenericServerResponse genericServerResponse = GenericServerResponse.builder().build();
    assertThat(genericServerResponse.isSuccess(), is(false));
    assertThat(genericServerResponse.getMessage(), nullValue());
  }

  @Test
  void isSuccess() {
    GenericServerResponse genericServerResponse =
        GenericServerResponse.builder().success(true).build();
    assertThat(genericServerResponse.isSuccess(), is(true));
    assertThat(GenericServerResponse.SUCCESS, equalToObject(genericServerResponse));
  }

  @Test
  void getMessage() {
    GenericServerResponse genericServerResponse =
        GenericServerResponse.builder().message("test").build();
    assertThat(genericServerResponse.getMessage(), equalTo("test"));
  }
}
