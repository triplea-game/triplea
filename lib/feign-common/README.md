## feign-common

Common core for building http clients with feign.

Http-clients will typically be per set of related methods
between a given client and server.

Http-clients will typically have 2 components:
(1) Feign annotated interface. This will describe the http
methods available, POST or GET, which parameters and headers.
(2) Request and response objects, these are converted to
JSON automatically.

## Notes and Conventions

  - each method in a feign interfaced will throw a `FeignException` if anything
goes wrong, including http 500's or IOException
  - Each feign interface should have a static convenience constructor
 method, eg: `ExampleFeignInterface.newClient(hostUri)`

## Testing notes

We use WireMock to set up a fake http server. We then create feign clients
to verify request/response sequences.

# Http Client List Overview and Summary

## Githhub Issues

API integration to create a github issue. We need an auth token for which we use
our bot account. Otherwise the API needs a title and body which we get from
the game user. The response from github contains a link to the newly created
issue.

## Lobby: Error Reporting

API that the lobby provides to accept data about exceptions. This lets users
uploads description data of an error with stack trace data automatically attached.
The lobby server in turn uploads this to github to create an issue ticket.
