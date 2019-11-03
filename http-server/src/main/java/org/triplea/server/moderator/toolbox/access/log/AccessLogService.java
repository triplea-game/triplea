package org.triplea.server.moderator.toolbox.access.log;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogData;
import org.triplea.lobby.server.db.dao.access.log.AccessLogDao;

@Builder
class AccessLogService {
  @Nonnull private final AccessLogDao accessLogDao;

  List<AccessLogData> fetchAccessLog(final PagingParams pagingParams) {
    return accessLogDao.fetchAccessLogRows(pagingParams.getRowNumber(), pagingParams.getPageSize())
        .stream()
        .map(
            daoData ->
                AccessLogData.builder()
                    .accessDate(daoData.getAccessTime())
                    .username(daoData.getUsername())
                    .ip(daoData.getIp())
                    .systemId(daoData.getSystemId())
                    .registered(daoData.isRegistered())
                    .build())
        .collect(Collectors.toList());
  }
}
