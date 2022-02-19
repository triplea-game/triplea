package org.triplea.http.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GenericServerResponseTest {

  @Test
  void builder() {
    GenericServerResponse genericServerResponse = GenericServerResponse.builder().build();
    assertFalse(genericServerResponse.isSuccess());
    assertNull(genericServerResponse.getMessage());
  }

  @Test
  void isSuccess() {
    GenericServerResponse genericServerResponseTrue =
        GenericServerResponse.builder().success(true).build();
    GenericServerResponse genericServerResponseFalse =
        GenericServerResponse.builder().success(false).build();
    assertTrue(genericServerResponseTrue.isSuccess());
    assertFalse(genericServerResponseFalse.isSuccess());
    assertEquals(GenericServerResponse.SUCCESS, genericServerResponseTrue);
  }

  @Test
  void getMessage() {
    GenericServerResponse genericServerResponse =
        GenericServerResponse.builder().message("test").build();
    assertEquals(genericServerResponse.getMessage(), "test");
  }
}
