package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpAddressExtractorTest {

  private static final String REMOTE_HOST = "Jacks travel from lives like scrawny clouds.";

  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void extractClientIp() {
    when(httpServletRequest.getRemoteAddr()).thenReturn(REMOTE_HOST);

    assertThat(IpAddressExtractor.extractClientIp(httpServletRequest), is(REMOTE_HOST));
  }
}
