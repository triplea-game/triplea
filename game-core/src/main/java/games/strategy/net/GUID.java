package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.dgc.VMID;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A globally unique id.
 * Backed by a java.rmi.dgc.VMID.
 * Written across the network often, so this class is externalizable to increase efficiency.
 */
public final class GUID implements Externalizable {
  private static final long serialVersionUID = 8426441559602874190L;
  // this prefix is unique across vms
  private static VMID vmPrefix = new VMID();
  // the local identifier
  // this coupled with the unique vm prefix comprise
  // our unique id
  private static AtomicInteger lastId = new AtomicInteger();
  private int id;
  private VMID prefix;

  public GUID() {
    id = lastId.getAndIncrement();
    prefix = vmPrefix;
    // handle wrap around if needed
    if (id < 0) {
      vmPrefix = new VMID();
      lastId = new AtomicInteger();
    }
  }

  public GUID(final VMID prefix, final int id) {
    checkNotNull(prefix);

    this.id = id;
    this.prefix = prefix;
  }

  public int getId() {
    return id;
  }

  public VMID getPrefix() {
    return prefix;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof GUID)) {
      return false;
    }
    final GUID other = (GUID) o;
    if (other == this) {
      return true;
    }
    return (this.id == other.id) && Objects.equals(this.prefix, other.prefix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, prefix);
  }

  @Override
  public String toString() {
    return "GUID:" + prefix + ":" + id;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    id = in.readInt();
    prefix = (VMID) in.readObject();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeInt(id);
    out.writeObject(prefix);
  }
}
