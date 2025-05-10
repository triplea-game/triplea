package games.strategy.net.nio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.net.TempPasswordHistory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordConversationTest {
  @NonNls private static final String USERNAME = "username";

  @Mock private Predicate<String> forgotPasswordModule;
  @Mock private TempPasswordHistory tempPasswordHistory;

  @InjectMocks private ForgotPasswordConversation forgotPasswordConversation;

  private static final InetAddress address;

  static {
    try {
      address = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void rejectIfLimitIsReached() {
    when(tempPasswordHistory.countRequestsFromAddress(address))
        .thenReturn(ForgotPasswordConversation.MAX_TEMP_PASSWORD_REQUESTS_PER_DAY);

    final String result = forgotPasswordConversation.handle(address, USERNAME);

    assertThat(result, is(not(ForgotPasswordConversation.TEMP_PASSWORD_GENERATED_RESPONSE)));

    verify(forgotPasswordModule, never()).test(any());
    verify(tempPasswordHistory, never()).recordTempPasswordRequest(any(), any());
  }

  @Test
  void doNotRecordHistoryIfFailToGenerate() {
    when(tempPasswordHistory.countRequestsFromAddress(address)).thenReturn(0);
    when(forgotPasswordModule.test(USERNAME)).thenReturn(false);

    final String result = forgotPasswordConversation.handle(address, USERNAME);

    assertThat(result, is(not(ForgotPasswordConversation.TEMP_PASSWORD_GENERATED_RESPONSE)));
    verify(tempPasswordHistory, never()).recordTempPasswordRequest(any(), any());
  }

  @Test
  void verifySuccessCase() {
    when(tempPasswordHistory.countRequestsFromAddress(address)).thenReturn(0);
    when(forgotPasswordModule.test(USERNAME)).thenReturn(true);

    final String result = forgotPasswordConversation.handle(address, USERNAME);

    assertThat(result, is(ForgotPasswordConversation.TEMP_PASSWORD_GENERATED_RESPONSE));
    verify(tempPasswordHistory).recordTempPasswordRequest(address, USERNAME);
  }
}
