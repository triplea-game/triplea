Test that checks the http client works, we use wiremock to simulate a server so we are not coupled
to any one server implementation. Server sub-projects should include the http-client as a test dependency
to then create an integration test to be sure that everything would work. Meanwhile we can test here
against a generic/stubbed server to be sure the client contract works as expected.

