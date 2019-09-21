package org.triplea.server.lobby.moderator.toolbox.access.log;

import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.AccessLogData;
import org.triplea.server.lobby.moderator.toolbox.ControllerTestUtil;

@ExtendWith(MockitoExtension.class)
class AccessLogControllerTest {
  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(100).rowNumber(0).build();

  @Mock private AccessLogService accessLogService;

  @InjectMocks private AccessLogController accessLogController;

  @Mock private AccessLogData accessLogData;

  @Test
  void fetchAccessLog() {
    when(accessLogService.fetchAccessLog(PAGING_PARAMS))
        .thenReturn(Collections.singletonList(accessLogData));

    final Response response = accessLogController.fetchAccessLog(PAGING_PARAMS);

    ControllerTestUtil.verifyResponse(response, Collections.singletonList(accessLogData));
  }
}
