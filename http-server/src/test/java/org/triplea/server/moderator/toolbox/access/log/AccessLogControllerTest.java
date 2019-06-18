package org.triplea.server.moderator.toolbox.access.log;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.AccessLogData;
import org.triplea.server.moderator.toolbox.ControllerTestUtil;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@ExtendWith(MockitoExtension.class)
class AccessLogControllerTest {
  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder()
          .pageSize(100)
          .rowNumber(0)
          .build();

  @Mock
  private ApiKeyValidationService apiKeyValidationService;
  @Mock
  private AccessLogService accessLogService;

  @InjectMocks
  private AccessLogController accessLogController;

  @Mock
  private HttpServletRequest request;
  @Mock
  private AccessLogData accessLogData;

  @Test
  void fetchAccessLog() {
    when(accessLogService.fetchAccessLog(PAGING_PARAMS))
        .thenReturn(Collections.singletonList(accessLogData));

    final Response response = accessLogController.fetchAccessLog(request, PAGING_PARAMS);

    ControllerTestUtil.verifyResponse(response, Collections.singletonList(accessLogData));
    verify(apiKeyValidationService).verifyApiKey(request);
  }
}
