Cookbook for creating an http endpoint and client

In this example we'll build a new Http service called: "TripleaService"

1. Create an HttpClient client class. This will wrap the FeignClient interface. Add a factory method:
```
TripleaServiceClient.newClient(URI serverUri);
```

Or, if the client should use an API key:
```
TripleaServiceClient.newClient(URI serverUri, String apiKey);
```

2. Add the FeignClient interface class that is package protected, eg: `TripleaServiceFeignClient`
This class will contain feign annotated methods and will only be accessed by `TripleaServiceClient`

3. Add a wiremock test: `TripleaServiceClientTest

4. Add a controller on the server, `TripleaServiceController`

5. Add a factory to construct the controller: `TripleaServiceControllerFactory.java`

6. Register the controller endpoint in jersey configuration: [ServerApplication.java](https://github.com/triplea-game/triplea/blob/master/http-server/src/main/java/org/triplea/server/http/ServerApplication.java)

7. Add a service class `TripleaService`, the controller should be responsible for all the HTTP "stuff" and be as thin as it can:
- extract args
- validate args are present
- invoke the service
- if the service does not return a response object, then wrap the service response in an http response

8. Add a unit test for the controller and implement the controller

9. Add a unit test for the service class and implement the service class

10. Add an integration test for the controller class
- Integration test should extent 'BasicEndpointTest if auth tokens are not used, or should extent 'ProtectedEndpointTest' if API tokens are used

### Best Practices

- Do thorough argument validation on the controller methods. Assert args are not null and look as valid as possible. (note, null args are not always properly flagged and errors at the controller level can cause the server to hang)
