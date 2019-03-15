package org.triplea.http.client;

import java.util.function.Function;

import lombok.RequiredArgsConstructor;

/**
 * Represents an http client connected to a single endpoint where you can send a request and get a response.
 *
 * @param <RequestT> The type sent to server.
 * @param <ResponseT> The type received from server.
 */
@RequiredArgsConstructor
public class ServiceClient<RequestT, ResponseT>
    implements Function<RequestT, ServiceResponse<ResponseT>> {

  private final HttpClient<?, RequestT, ResponseT> client;

  @Override
  public ServiceResponse<ResponseT> apply(
      final RequestT requestToSend) {

    return client.apply(requestToSend);
  }
}
