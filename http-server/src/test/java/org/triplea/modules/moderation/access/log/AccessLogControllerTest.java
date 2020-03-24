package org.triplea.modules.moderation.access.log;

import static org.mockito.Mockito.when;

import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.ControllerTestUtil;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogData;

@ExtendWith(MockitoExtension.class)
class AccessLogControllerTest {
  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder().pageSize(100).rowNumber(0).build();

  @Mock private AccessLogService accessLogService;

  @InjectMocks private AccessLogController accessLogController;

  @Mock private AccessLogData accessLogData;

  @Test
  void fetchAccessLog() {
    when(accessLogService.fetchAccessLog(PAGING_PARAMS)).thenReturn(List.of(accessLogData));

    final Response response = accessLogController.fetchAccessLog(PAGING_PARAMS);

    ControllerTestUtil.verifyResponse(response, List.of(accessLogData));
  }
}
