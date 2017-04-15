package games.strategy.persistence.serializable;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectStreamClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class ObjectInputStreamTest {
  private ObjectInputStream ois;

  @Mock
  private PersistenceDelegate persistenceDelegate;

  private final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();

  @Before
  public void setUp() throws Exception {
    ois = new ObjectInputStream(newNonEmptyInputStream(), persistenceDelegateRegistry);
  }

  private InputStream newNonEmptyInputStream() throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (final ObjectOutputStream oos = new ObjectOutputStream(baos, persistenceDelegateRegistry)) {
        oos.writeObject(Integer.valueOf(42));
      }
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }

  @After
  public void tearDown() throws Exception {
    ois.close();
  }

  @Test
  public void resolveClass_ShouldDelegateToPersistenceDelegateWhenPersistenceDelegateAvailable() throws Exception {
    final ObjectStreamClass desc = ObjectStreamClass.lookup(Integer.class);
    persistenceDelegateRegistry.registerPersistenceDelegate(desc.forClass(), persistenceDelegate);

    ois.resolveClass(desc);

    verify(persistenceDelegate).resolveClass(ois, desc);
  }

  @Test
  public void resolveClass_ShouldNotThrowExceptionWhenPersistenceDelegateUnavailable() throws Exception {
    ois.resolveClass(ObjectStreamClass.lookup(Integer.class));
  }

  @Test
  public void resolveObject_ShouldDelegateToPersistenceDelegateWhenPersistenceDelegateAvailable() throws Exception {
    final Object obj = Integer.valueOf(42);
    persistenceDelegateRegistry.registerPersistenceDelegate(Integer.class, persistenceDelegate);

    ois.resolveObject(obj);

    verify(persistenceDelegate).resolveObject(obj);
  }

  @Test
  public void resolveObject_ShouldNotThrowExceptionWhenPersistenceDelegateUnavailable() throws Exception {
    ois.resolveObject(Integer.valueOf(42));
  }

  @Test
  public void resolveObject_ShouldReturnNullWhenObjectIsNull() throws Exception {
    assertThat(ois.resolveObject(null), is(nullValue()));
  }
}
