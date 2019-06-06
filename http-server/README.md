# Http-Server

## Strategic Fit and Overview

Http-Server is an http/REST-like backend component to complement the lobby.
The server is intended to provide a lightweight way to do client/server
interactions without needing to use the Lobby java sockets interface.

The server is intended to be run as a stand-alone process and may access
the same database as the lobby.

## Starting the server
Execute the main method in `ServerApplication`. If no args are provided then
defaults suitable for a prerelease/development are used.

### Environment Variables

For full functionality, secrets are provided via environment variables and need
to be set prior to launching the server. See the `configuration-prerelease.yml`
for those environment variable names. By default the server should be launchable
without any additional configuration, but some elements of the system may not function
without valid values.

## Configuration

Application configuration is obtained from `AppConfig.java` which is wired
from a YML file that is specified at startup. The prerelease and production
YML files may differ in the values of configuration properties, but otherwise
should have the same number of configuration properites, the same keys
and the same environment variables.

Of note, a reference to `AppConfig` is passed to the main server application
`ServerApplication` which can then wire those properties to any endpoint
'controllers' that would need configuration values.



## Sample Curl Requests for local Testing


### Error reporting

```bash
curl -X POST  -H "Content-Type: application/json" \
  -d '{"title":"my-title", "body":"my-body"}' \
  localhost:8080/error-report
```

```bash
curl -X GET -H "Content-Type: application/json" \
  localhost:8080/can-submit-error-report
```


# TODO

- [ ] create an integration test that will simply launch the server to verify we have a 
  valid configuration.
- [ ] see if we can create an integration test with mocked out backend that
  can verify http server/client interactions

# WARNINGS!

## Multiple SLF4J Bindings Not Allowed

Dropwizard uses Logback and has a binding with SLF4J baked in. Additional SLF4J bindings 
should generate a warning, but will ultimately cause problems (when run from gradle) and drop 
wizard may fail to start with this error:

```bash
java.lang.IllegalStateException: Unable to acquire the logger context
    at io.dropwizard.logging.LoggingUtil.getLoggerContext(LoggingUtil.java:46)
```

## Stream is already closed

When submitting a request with improper params, if there validation is not quite
proper then the server can hit this error:
```bash
ERROR [2019-06-06 05:07:22,247] org.glassfish.jersey.server.ServerRuntime$Responder: An I/O error has occurred while writing a response message entity to the container output stream.
! java.lang.IllegalStateException: The output stream has already been closed.
```

The impact of this is: 
- server thread hangs
- client hangs
- server does not shutdown cleanly

This is bad as it could be used as a DDOS attack.

### Prevention

Essentially fail-fast:
- When looking for headers, verify headers exist or terminate the request
- Verify that all needed GET parameters are present