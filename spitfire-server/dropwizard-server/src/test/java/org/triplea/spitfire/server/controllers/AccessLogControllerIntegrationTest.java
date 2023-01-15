package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.AccessLogSearchRequest;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class AccessLogControllerIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final ToolboxAccessLogClient client;

  AccessLogControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    this.client = ToolboxAccessLogClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ToolboxAccessLogClient.newClient(localhost, apiKey),
        client ->
            client.getAccessLog(
                AccessLogSearchRequest.builder().username("username").ip("ip").build(),
                PagingParams.builder().pageSize(1).rowNumber(0).build()),
        client -> client.getAccessLog(PagingParams.builder().pageSize(1).rowNumber(0).build()));
  }

  @Test
  void getAccessLog() {
    final var result = client.getAccessLog(PagingParams.builder().pageSize(1).rowNumber(0).build());

    assertThat(result, is(not(empty())));
    assertThat(result.get(0).getAccessDate(), is(notNullValue()));
    assertThat(result.get(0).getSystemId(), is(notNullValue()));
    assertThat(result.get(0).getUsername(), is(notNullValue()));
  }

  @Test
  void getAccessLogUnauthorizedCase() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ToolboxAccessLogClient.newClient(localhost, apiKey),
        client -> client.getAccessLog(PagingParams.builder().pageSize(1).rowNumber(0).build()));
  }

  /**
   * First we do a fetch of access log and we pick the first record. We then search by username and
   * IP address using data from the first record, which should guarantee that our search will return
   * at least that one result (if not more).
   */
  @Test
  void getAccessLogWithSearchParams() {
    // first search for a user-name and IP that exist in the system
    final var firstListing =
        client.getAccessLog(PagingParams.builder().pageSize(1).rowNumber(0).build()).get(0);

    final var result =
        client.getAccessLog(
            AccessLogSearchRequest.builder()
                .username(firstListing.getUsername())
                .ip(firstListing.getIp())
                .build(),
            PagingParams.builder().pageSize(1).rowNumber(0).build());

    assertThat(
        "We expect there to have been at least one match for sure", result, is(not(empty())));
    assertThat(
        "Username should match what we searched for",
        result.get(0).getUsername(),
        is(firstListing.getUsername()));
    assertThat(
        "IP should match what we searched for", result.get(0).getIp(), is(firstListing.getIp()));
  }

  @Test
  void emptySearchShouldBeSameAsAllSearch() {
    // do a search with empty search parameters (should return everything)
    final var emptySearchResult =
        client.getAccessLog(
            AccessLogSearchRequest.builder().build(),
            PagingParams.builder().pageSize(1).rowNumber(0).build());

    // do a full listing of all records
    final var allSearchResult =
        client.getAccessLog(PagingParams.builder().pageSize(1).rowNumber(0).build());

    assertThat(emptySearchResult, is(equalTo(allSearchResult)));
  }
}
