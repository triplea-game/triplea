package org.triplea.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Util class for controller tests. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ControllerTestUtil {
  public static void verifyResponse(final Response response, final Object expectedEntity) {
    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(expectedEntity));
  }
}
