package org.triplea.server.error.reporting;

import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import lombok.Builder;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.error.report.ErrorUploadResponse;
import org.triplea.server.http.IpAddressExtractor;

/** Http controller that binds the error upload endpoint with the error report upload handler. */
@Builder
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public class ErrorReportController {
  @Nonnull private final Function<ErrorReportRequest, ErrorUploadResponse> errorReportIngestion;

  @POST
  @Path(ErrorUploadClient.ERROR_REPORT_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {
        @Rate(limit = ErrorUploadClient.MAX_REPORTS_PER_DAY, duration = 1, timeUnit = TimeUnit.DAYS)
      })
  public ErrorUploadResponse uploadErrorReport(
      @Context final HttpServletRequest request, final ErrorUploadRequest errorReport) {

    if (errorReport.getBody() == null || errorReport.getTitle() == null) {
      throw new IllegalArgumentException("Missing error report body and/or title");
    }

    return errorReportIngestion.apply(
        ErrorReportRequest.builder()
            .clientIp(IpAddressExtractor.extractClientIp(request))
            .errorReport(errorReport)
            .build());
  }
}
