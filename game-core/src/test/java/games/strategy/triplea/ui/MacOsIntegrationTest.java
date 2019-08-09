package games.strategy.triplea.ui;

import static games.strategy.triplea.ui.MacOsIntegration.isJavaVersionAtLeast9;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.function.ThrowingConsumer;

final class MacOsIntegrationTest {
  @Nested
  final class IsJavaVersionAtLeast9Test {
    @Test
    void shouldReturnTrueWhenJavaVersionIs9OrLater() {
      Arrays.asList("9", "10", "11", "12")
          .forEach(
              specificationVersion ->
                  assertThat(isJavaVersionAtLeast9(specificationVersion), is(true)));
    }

    @Test
    void shouldReturnFalseWhenJavaVersionIsMalformed() {
      assertThat(isJavaVersionAtLeast9(""), is(false));
    }

    @Test
    void shouldReturnFalseWhenJavaVersionIs8() {
      assertThat(isJavaVersionAtLeast9("1.8"), is(false));
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class AddHandlerTest {
    private final FakeApplication application = new FakeApplication();
    @Mock private ThrowingConsumer<Object[], Exception> handler;

    private void addHandler() throws ReflectiveOperationException {
      addHandlerWithHandlerMethodName("handleRequest");
    }

    private void addHandlerWithHandlerMethodName(final String handlerMethodName)
        throws ReflectiveOperationException {
      MacOsIntegration.addHandler(
          application, FakeHandler.class.getName(), handlerMethodName, "setFakeHandler", handler);
    }

    @Test
    void shouldAddHandlerWhenRequestIsValid() throws Exception {
      addHandler();

      assertThat(application, is(not(nullValue())));
      assertThat(application.handler, is(not(nullValue())));
    }

    @Test
    void shouldInvokeHandlerMethodWhenMethodNameMatches() throws Exception {
      addHandler();

      application.handler.handleRequest("value");

      verify(handler).accept(new Object[] {"value"});
    }

    @Test
    void shouldNotInvokeHandlerMethodWhenMethodNameDoesNotMatch() throws Exception {
      addHandlerWithHandlerMethodName("__unknownMethodName__");

      application.handler.handleRequest("value");

      verify(handler, never()).accept(any());
    }
  }

  private static final class FakeApplication {
    FakeHandler handler;

    @SuppressWarnings("unused") // invoked via reflection
    public void setFakeHandler(final FakeHandler handler) {
      this.handler = handler;
    }
  }

  private interface FakeHandler {
    void handleRequest(String arg);
  }
}
