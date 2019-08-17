package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.io.ObjectInputStream;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/** Default implementation of {@link Named} for game data components. */
@EqualsAndHashCode(callSuper = false)
public class DefaultNamed extends GameDataComponent implements Named {
  private static final long serialVersionUID = -5737716450699952621L;

  @Getter(onMethod_ = {@Override})
  private final String name;

  public DefaultNamed(final String name, final GameData data) {
    super(data);

    checkNotNull(name);
    if (name.length() == 0) {
      throw new IllegalArgumentException("Name must not be empty");
    }

    this.name = name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("name", name).toString();
  }

  // Workaround for JDK-8199664
  @SuppressWarnings("static-method")
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }
}
