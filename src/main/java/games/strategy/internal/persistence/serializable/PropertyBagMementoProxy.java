package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

import games.strategy.persistence.serializable.VersionedProxySupport;
import games.strategy.util.memento.PropertyBagMemento;

public final class PropertyBagMementoProxy implements Externalizable {
  private static final long serialVersionUID = 7813364982800353383L;

  private static final long CURRENT_VERSION = 1L;

  private final VersionedProxySupport versionedProxySupport = new VersionedProxySupport(this);

  /**
   * @serial The collection of originator properties; never {@code null}. The key is the property name. The value is the
   *         property value.
   */
  private Map<String, Object> propertiesByName;

  /**
   * @serial The memento schema identifier; never {@code null}.
   */
  private String schemaId;

  /**
   * @serial The memento schema version.
   */
  private long schemaVersion;

  /**
   * Initializes a new instance of the {@code PropertyBagMementoProxy} class during deserialization.
   */
  public PropertyBagMementoProxy() {}

  /**
   * Initializes a new instance of the {@code PropertyBagMementoProxy} class from the specified
   * {@code PropertyBagMemento} instance.
   *
   * @param memento The {@code PropertyBagMemento} instance; must not be {@code null}.
   */
  public PropertyBagMementoProxy(final PropertyBagMemento memento) {
    checkNotNull(memento);

    propertiesByName = memento.getPropertiesByName();
    schemaId = memento.getSchemaId();
    schemaVersion = memento.getSchemaVersion();
  }

  private Object readResolve() {
    return new PropertyBagMemento(schemaId, schemaVersion, propertiesByName);
  }

  @SuppressWarnings("unused")
  private void readExternalV1(final ObjectInput in) throws IOException, ClassNotFoundException {
    schemaId = in.readUTF();
    schemaVersion = in.readLong();
    @SuppressWarnings("unchecked")
    final Map<String, Object> propertiesByName = (Map<String, Object>) in.readObject();
    this.propertiesByName = propertiesByName;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    versionedProxySupport.read(in);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    versionedProxySupport.write(out, CURRENT_VERSION);
  }

  @SuppressWarnings("unused")
  private void writeExternalV1(final ObjectOutput out) throws IOException {
    out.writeUTF(schemaId);
    out.writeLong(schemaVersion);
    out.writeObject(propertiesByName);
  }
}
