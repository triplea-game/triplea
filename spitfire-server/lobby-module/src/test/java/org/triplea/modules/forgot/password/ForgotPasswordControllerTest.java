package org.triplea.modules.forgot.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.http.client.forgot.password.ForgotPasswordResponse;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordControllerTest {

  private static final String IP_ADDRESS = "ip";
  private static final String RESPONSE = "Never hail a sea dog.";

  private static final ForgotPasswordRequest forgotPasswordRequest =
      ForgotPasswordRequest.builder().username("user").email("email").build();

  @Mock private ForgotPasswordModule forgotPasswordModule;

  @InjectMocks private ForgotPasswordController forgotPasswordController;

  @Mock private HttpServletRequest httpServletRequest;

  @Test
  void illegalArgCases() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            forgotPasswordController.requestTempPassword(
                httpServletRequest,
                ForgotPasswordRequest.builder().username(null).email("email").build()));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            forgotPasswordController.requestTempPassword(
                httpServletRequest,
                ForgotPasswordRequest.builder().username("username").email(null).build()));
  }

  @Test
  void requestTempPassword() {
    when(httpServletRequest.getRemoteAddr()).thenReturn(IP_ADDRESS);

    when(forgotPasswordModule.apply(IP_ADDRESS, forgotPasswordRequest)).thenReturn(RESPONSE);

    final ForgotPasswordResponse response =
        forgotPasswordController.requestTempPassword(httpServletRequest, forgotPasswordRequest);

    assertThat(response.getResponseMessage(), is(RESPONSE));
  }
}
