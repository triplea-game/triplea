package org.triplea.server.moderator.toolbox.audit.history;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
class ModeratorAuditHistoryControllerTest {

  private static final ModeratorEvent EVENT_1 = ModeratorEvent.builder()
      .date(Instant.now())
      .actionTarget("Desolation is a salty corsair.")
      .moderatorAction("Jolly, small grace!")
      .moderatorName("Yardarms are the pants of the golden greed.")
      .build();

  private static final int ROW_NUMBER = 60;
  private static final int ROW_COUNT = 50;


  @Mock
  private ApiKeySecurityService apiKeySecurityService;
  @Mock
  private ApiKeyValidationService apiKeyValidationService;
  @Mock
  private ModeratorAuditHistoryService moderatorAuditHistoryService;

  @InjectMocks
  private ModeratorAuditHistoryController moderatorAuditHistoryController;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Test
  void verifiesGetParameters() {
    assertThrows(IllegalArgumentException.class,
        () -> moderatorAuditHistoryController.lookupAuditHistory(httpServletRequest, null, 1));
    assertThrows(IllegalArgumentException.class,
        () -> moderatorAuditHistoryController.lookupAuditHistory(httpServletRequest, 1, null));
  }

  @Test
  void lookupAuditHistoryLockedOut() {
    when(apiKeySecurityService.allowValidation(httpServletRequest)).thenReturn(false);

    final Response response = moderatorAuditHistoryController.lookupAuditHistory(httpServletRequest, 1, 1);

    assertThat(response.getStatus(), is(ApiKeyValidationService.LOCK_OUT_RESPONSE.getStatus()));
  }

  @Test
  void lookupAuditHistoryInvalidKey() {
    when(apiKeySecurityService.allowValidation(httpServletRequest)).thenReturn(true);
    when(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest)).thenReturn(Optional.empty());

    final Response response = moderatorAuditHistoryController.lookupAuditHistory(httpServletRequest, 1, 1);

    assertThat(response.getStatus(), is(ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE.getStatus()));
  }

  @Test
  void lookupAuditHistory() {
    when(apiKeySecurityService.allowValidation(httpServletRequest)).thenReturn(true);
    when(apiKeyValidationService.lookupModeratorIdByApiKey(httpServletRequest)).thenReturn(Optional.of(1));

    when(moderatorAuditHistoryService.lookupHistory(ROW_NUMBER, ROW_COUNT)).thenReturn(
        ImmutableList.of(EVENT_1));

    final Response response =
        moderatorAuditHistoryController.lookupAuditHistory(httpServletRequest, ROW_NUMBER, ROW_COUNT);

    assertThat(response.getStatus(), is(200));
    assertThat(((List) response.getEntity()).get(0), is(EVENT_1));
  }
}
