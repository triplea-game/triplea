package org.triplea.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class PublicIdSupplierTest {

  private static final Supplier<String> supplier = new PublicIdSupplier();

  @Test
  void uniqueKeys() {
    final String value1 = supplier.get();
    final String value2 = supplier.get();

    assertThat(value1, not(equalTo(value2)));
  }

  @Test
  void verifyLength() {
    final String value = supplier.get();
    assertThat(value.length() > 16, is(true));
  }
}
