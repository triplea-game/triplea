package org.triplea.modules.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.CanUploadErrorReportResponse;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.server.error.reporting.upload.CanUploadErrorReportStrategy;
import org.triplea.server.error.reporting.upload.ErrorReportingDao;

@ExtendWith(MockitoExtension.class)
class CanUploadErrorReportStrategyTest {

  @Mock private ErrorReportingDao errorReportingDao;

  @InjectMocks private CanUploadErrorReportStrategy canUploadErrorReportStrategy;

  @Test
  @DisplayName("If we do not find an existing error report then the user can upload")
  void errorReportDoesNotExist() {
    when(errorReportingDao.getErrorReportLink("reportTitle", "version"))
        .thenReturn(Optional.empty());

    final CanUploadErrorReportResponse response =
        canUploadErrorReportStrategy.apply(
            CanUploadRequest.builder().errorTitle("reportTitle").gameVersion("version").build());

    assertThat(response.getCanUpload(), is(true));
    assertThat(response.getExistingBugReportUrl(), is(nullValue()));
    assertThat(response.getResponseDetails(), is(nullValue()));
  }

  @Test
  @DisplayName("If find an existing error report then the user can *not* upload")
  void errorReportDoesExist() {
    when(errorReportingDao.getErrorReportLink("reportTitle", "version"))
        .thenReturn(Optional.of("link"));

    final CanUploadErrorReportResponse response =
        canUploadErrorReportStrategy.apply(
            CanUploadRequest.builder().errorTitle("reportTitle").gameVersion("version").build());

    assertThat(response.getCanUpload(), is(false));
    assertThat(response.getExistingBugReportUrl(), is("link"));
    assertThat(response.getResponseDetails(), is(notNullValue()));
  }
}
