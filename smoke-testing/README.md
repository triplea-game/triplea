Project that contains 'test-only' code meant for basic verification of live running components.

In essence the goal of the smoke testing will be to stand up a lobby and bot server and see if we can establish a connection.

The goal of this testing is not to verify functionality, or logic nor correctness, but to ensure that basic configurations are in place, that we can send typical requests and not get any errors. The other layers of testing, unit and integration testing are meant to ensure that exact interaction sequences are correct or that we get correct data. This testing is meant to send requests and verify that they can flow through the stack and not get any errors.

