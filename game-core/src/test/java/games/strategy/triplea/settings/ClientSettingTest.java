package games.strategy.triplea.settings;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;

import com.example.mockito.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientSettingTest extends AbstractClientSettingTestCase {

  private static final String TEST_VALUE = "value";

  @Mock
  private Consumer<String> mockSaveListener;

  @Test
  public void saveActionListenerIsCalled() {
    ClientSetting.TEST_SETTING.addSaveListener(mockSaveListener);

    ClientSetting.TEST_SETTING.save(TEST_VALUE);

    Mockito.verify(mockSaveListener, Mockito.times(1))
        .accept(TEST_VALUE);
  }

  @Test
  public void verifyRemovedSavedListenersAreNotCalled() {
    ClientSetting.TEST_SETTING.addSaveListener(mockSaveListener);
    ClientSetting.TEST_SETTING.removeSaveListener(mockSaveListener);

    ClientSetting.TEST_SETTING.save(TEST_VALUE);

    Mockito.verify(mockSaveListener, Mockito.never())
        .accept(TEST_VALUE);
  }
}
