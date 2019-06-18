package org.triplea.server.moderator.toolbox.access.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.AccessLogData;
import org.triplea.lobby.server.db.dao.AccessLogDao;
import org.triplea.lobby.server.db.data.AccessLogDaoData;

@ExtendWith(MockitoExtension.class)
class AccessLogServiceTest {
  private static final PagingParams PAGING_PARAMS =
      PagingParams.builder()
          .pageSize(100)
          .rowNumber(0)
          .build();

  private static final AccessLogDaoData ACCESS_LOG_DAO_DATA =
      AccessLogDaoData.builder()
          .accessTime(Instant.now())
          .username("username")
          .ip("ip")
          .mac("hashed-mac")
          .registered(true)
          .build();

  @Mock
  private AccessLogDao accessLogDao;

  @InjectMocks
  private AccessLogService accessLogService;

  @Test
  void fetchAccessLog() {
    when(accessLogDao.lookupAccessLogData(
        PAGING_PARAMS.getRowNumber(), PAGING_PARAMS.getPageSize()))
            .thenReturn(Collections.singletonList(ACCESS_LOG_DAO_DATA));

    final List<AccessLogData> results = accessLogService.fetchAccessLog(PAGING_PARAMS);

    assertThat(results, hasSize(1));

    assertThat(results.get(0).getAccessDate(), is(ACCESS_LOG_DAO_DATA.getAccessTime()));
    assertThat(results.get(0).getHashedMac(), is(ACCESS_LOG_DAO_DATA.getMac()));
    assertThat(results.get(0).getIp(), is(ACCESS_LOG_DAO_DATA.getIp()));
    assertThat(results.get(0).getUsername(), is(ACCESS_LOG_DAO_DATA.getUsername()));
    assertThat(results.get(0).isRegistered(), is(ACCESS_LOG_DAO_DATA.isRegistered()));
  }
}
