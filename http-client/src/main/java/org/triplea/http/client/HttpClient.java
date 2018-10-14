package org.triplea.http.client;

import java.net.URI;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper around feign clients to do provide universal http send/receive functionality.
 * Notably this class provides a unified interface for throttling, for interpretting errors/exceptions
 * or status code 500's.
 *
 * @param <ClientTypeT> The feign client interface, should be an interface type that has feign annotations on it.
 * @param <RequestT> The data type we are sending to the server, will be converted to a JSON.
 * @param <ResponseT> Data type coming back from server.
 */
// TODO: verify testing.
@RequiredArgsConstructor
public class HttpClient<ClientTypeT, RequestT, ResponseT>
    implements BiFunction<URI, RequestT, ServiceResponse<ResponseT>> {

  private final Consumer<RequestT> rateLimiter = new RateLimiter<>();
  private final Class<ClientTypeT> classType;
  private final BiFunction<ClientTypeT, RequestT, ResponseT> sendFunction;

  @Override
  public ServiceResponse<ResponseT> apply(
      final URI hostUri,
      final RequestT requestToSend) {
    try {
      rateLimiter.accept(requestToSend);
    } catch (final RuntimeException e) {
      return ServiceResponse.<ResponseT>builder()
          .sendResult(SendResult.NOT_SENT)
          .thrown(e)
          .build();
    }

    try {
      final ClientTypeT clientInterface = FeignFactory.build(classType, hostUri);
      final ResponseT responseFromServer = sendFunction.apply(clientInterface, requestToSend);
      return ServiceResponse.<ResponseT>builder()
          .sendResult(SendResult.SENT)
          .payload(responseFromServer)
          .build();
    } catch (final FeignException e) {
      return ServiceResponse.<ResponseT>builder()
          .sendResult(e.status() > 0 && e.status() < 500 ? SendResult.CLIENT_ERROR : SendResult.SERVER_ERROR)
          .thrown(e)
          .build();
    } catch (final RuntimeException e) {
      return ServiceResponse.<ResponseT>builder()
          .sendResult(SendResult.COMMUNICATION_ERROR)
          .thrown(e)
          .build();
    }
  }
}
