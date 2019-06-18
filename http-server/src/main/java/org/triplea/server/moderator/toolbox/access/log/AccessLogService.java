package org.triplea.server.moderator.toolbox.access.log;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.triplea.http.client.moderator.toolbox.PagingParams;
import org.triplea.http.client.moderator.toolbox.access.log.AccessLogData;
import org.triplea.lobby.server.db.dao.AccessLogDao;

import lombok.Builder;

@Builder
class AccessLogService {
  @Nonnull
  private final AccessLogDao accessLogDao;

  List<AccessLogData> fetchAccessLog(final PagingParams pagingParams) {
    return accessLogDao.lookupAccessLogData(
        pagingParams.getRowNumber(), pagingParams.getPageSize())
        .stream()
        .map(daoData -> AccessLogData.builder()
            .accessDate(daoData.getAccessTime())
            .username(daoData.getUsername())
            .ip(daoData.getIp())
            .hashedMac(daoData.getMac())
            .registered(daoData.isRegistered())
            .build())
        .collect(Collectors.toList());
  }
}
