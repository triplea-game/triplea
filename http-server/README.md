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
  -d '{"title":"my-title", "body":"my-body"}' localhost:8080/error-report
```


# TODO

- [ ] create an integration test that will simply launch the server to verify we have a 
  valid configuration.
- [ ] see if we can create an integration test with mocked out backend that
  can verify http server/client interactions
