package org.triplea.server.http.spark;

import static spark.Spark.post;

import java.time.Instant;
import java.util.function.Supplier;

import org.triplea.server.reporting.error.upload.ErrorReport;
import org.triplea.server.reporting.error.upload.ErrorReportIngestion;

import com.google.common.annotations.VisibleForTesting;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import spark.Request;

/**
 * Class that handles the Spark/http server related responsibilites. This includes
 * binding URL endpoints to behavior objects/methods, and secondly marshalling request
 * data into objects which can then be passed along and processed.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class SparkServer {
  @VisibleForTesting
  public static final String ERROR_REPORT_PATH = "/error_report";
  private final ErrorReportIngestion errorReportIngestion;
  private final Supplier<Instant> clock;


  public static void main(final String[] args) {
    final SparkServer sparkServerMain = SparkServer.builder()
        .errorReportIngestion(new ErrorReportIngestion())
        .clock(Instant::now)
        .build();

    post(ERROR_REPORT_PATH, (req, res) -> sparkServerMain.uploadErrorReport(req));
  }

  String uploadErrorReport(final Request req) {
    final ErrorReport errorReport = readErrorReport(req);
    errorReportIngestion.reportError(errorReport);
    return "SUCCESS";
  }

  private ErrorReport readErrorReport(final Request request) {
    return ErrorReport.builder()
        .reportedOn(clock.get())
        .reportContents(request.body())
        .reportingHostId(formatHostId(request.host(), request.ip()))
        .build();
  }

  @VisibleForTesting
  static String formatHostId(final String host, final String ip) {
    return host + "-" + ip;
  }
}
