package games.strategy.triplea.settings;

import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.jqno.equalsverifier.EqualsVerifier;

final class ClientSettingTest {
  @Nested
  final class EqualsAndHashCodeTest {
    @Test
    void shouldBeEquatableAndHashable() {
      EqualsVerifier.forClass(ClientSetting.class)
          .withNonnullFields("name")
          .withOnlyTheseFields("name")
          .verify();
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class SaveActionTest extends AbstractClientSettingTestCase {
    private static final String TEST_VALUE = "value";

    @Mock
    private Consumer<String> mockSaveListener;
    private final ClientSetting<String> clientSetting = new StringClientSetting("TEST_SETTING");

    @Test
    void saveActionListenerIsCalled() {
      clientSetting.addSaveListener(mockSaveListener);

      clientSetting.save(TEST_VALUE);

      Mockito.verify(mockSaveListener, Mockito.times(1))
          .accept(TEST_VALUE);
    }

    @Test
    void verifyRemovedSavedListenersAreNotCalled() {
      clientSetting.addSaveListener(mockSaveListener);
      clientSetting.removeSaveListener(mockSaveListener);

      clientSetting.save(TEST_VALUE);

      Mockito.verify(mockSaveListener, Mockito.never())
          .accept(TEST_VALUE);
    }
  }
}
