package org.triplea.http.client;

import java.net.URI;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;

/**
 * Represents an http client connected to a single endpoint where you can send a request and get a response.
 *
 * @param <RequestT> The type sent to server.
 * @param <ResponseT> The type received from server.
 */
@RequiredArgsConstructor
public class ServiceClient<RequestT, ResponseT>
    implements BiFunction<URI, RequestT, ServiceResponse<ResponseT>> {

  private final HttpClient<?, RequestT, ResponseT> client;

  @Override
  public ServiceResponse<ResponseT> apply(
      final URI hostUri,
      final RequestT requestToSend) {

    return client.apply(hostUri, requestToSend);
  }
}
