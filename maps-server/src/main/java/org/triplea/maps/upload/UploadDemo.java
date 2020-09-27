package org.triplea.maps.upload;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;

@Slf4j
public class UploadDemo {
  public static void main(String[] args) throws Exception{
    String url = "http://localhost:9090/maps/upload/test.txt";
    String body = "BODY OF THE FILE";

    HttpEntity entity = MultipartEntityBuilder
        .create()
        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        .addBinaryBody("file", body.getBytes())
        .build();

    HttpClient client =HttpClient.newHttpClient();
    HttpRequest httpRequest = HttpRequest.newBuilder()
        .PUT(HttpRequest.BodyPublishers.ofByteArray(body.getBytes()))
        .uri(URI.create(url))
//        .header()
        .build();


    HttpResponse<String> response =
        client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    log.info("status: " + response.statusCode());
    log.info("headers: " + response.headers());
    log.info("response: " + response.body());
  }
}
