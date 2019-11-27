package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.java.ArgChecker;

/** Default implementation of {@link Named} for game data components. */
@EqualsAndHashCode(callSuper = false)
@ToString
public class DefaultNamed extends GameDataComponent implements Named {
  private static final long serialVersionUID = -5737716450699952621L;

  @Getter(onMethod_ = @Override)
  private final String name;

  public DefaultNamed(final String name, final GameData data) {
    super(data);
    ArgChecker.checkNotEmpty(name);
    this.name = name;
  }

  // Workaround for JDK-8199664
  @SuppressWarnings("static-method")
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }
}
