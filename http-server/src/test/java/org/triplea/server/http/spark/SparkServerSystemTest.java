package org.triplea.server.http.spark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

import org.junit.jupiter.api.Test;

import org.triplea.test.common.Integration;

import spark.Spark;


@Integration
class SparkServerSystemTest {

  private static final int SPARK_PORT = 5000;

  @Test
  void startServerAndStopIt() throws Exception {
    Spark.port(SPARK_PORT);
    SparkServer.main(new String[] {});
    Spark.awaitInitialization();

    MatcherAssert.assertThat(sendRequest(), Is.is("SUCCESS"));

    Spark.stop();
  }

  private static String sendRequest() throws Exception {
    final URL url = new URL("http://localhost:" + SPARK_PORT + SparkServer.ERROR_REPORT_PATH);
    final HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestMethod("POST");

    try (
        final InputStreamReader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
        final BufferedReader in = new BufferedReader(reader)) {
      String inputLine;
      final StringBuilder content = new StringBuilder();
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      return content.toString();
    }
  }
}
