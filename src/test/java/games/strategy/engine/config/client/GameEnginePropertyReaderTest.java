package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.config.PropertyFileReader;
import games.strategy.util.Version;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class GameEnginePropertyReaderTest {

  @Mock
  private PropertyFileReader mockPropertyFileReader;

  @InjectMocks
  private GameEnginePropertyReader testObj;

  @Test
  public void engineVersion() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.ENGINE_VERSION))
        .thenReturn("1.0.1.3");

    assertThat(testObj.getEngineVersion(), is(new Version(1, 0, 1, 3)));
  }

  @Test
  public void useNewSaveGameFormat_ShouldReturnTrueWhenPropertyValueIsCaseSensitiveTrue() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.NEW_SAVE_GAME_FORMAT))
        .thenReturn("true");

    assertThat(testObj.useNewSaveGameFormat(), is(true));
  }

  @Test
  public void useNewSaveGameFormat_ShouldReturnTrueWhenPropertyValueIsCaseInsensitiveTrue() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.NEW_SAVE_GAME_FORMAT))
        .thenReturn("True");

    assertThat(testObj.useNewSaveGameFormat(), is(true));
  }

  @Test
  public void useNewSaveGameFormat_ShouldReturnFalseWhenPropertyValueIsAbsent() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.NEW_SAVE_GAME_FORMAT))
        .thenReturn("");

    assertThat(testObj.useNewSaveGameFormat(), is(false));
  }
}
