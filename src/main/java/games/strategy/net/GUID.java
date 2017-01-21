package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.rmi.dgc.VMID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A globally unique id. <br>
 * Backed by a java.rmi.dgc.VMID.
 * Written across the network often, so this class is
 * externalizable to increase effeciency
 */
public class GUID implements Externalizable {
  private static final long serialVersionUID = 8426441559602874190L;
  // this prefix is unique across vms
  private static VMID vm_prefix = new java.rmi.dgc.VMID();
  // the local identifier
  // this coupled with the unique vm prefix comprise
  // our unique id
  private static AtomicInteger s_lastID = new AtomicInteger();
  private int m_id;
  private VMID m_prefix;

  public GUID() {
    m_id = s_lastID.getAndIncrement();
    m_prefix = vm_prefix;
    // handle wrap around if needed
    if (m_id < 0) {
      vm_prefix = new VMID();
      s_lastID = new AtomicInteger();
    }
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
    return this.m_id == other.m_id && (other.m_prefix == this.m_prefix || other.m_prefix.equals(this.m_prefix));
  }

  @Override
  public int hashCode() {
    return m_id ^ m_prefix.hashCode();
  }

  @Override
  public String toString() {
    return "GUID:" + m_prefix + ":" + m_id;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    m_id = in.readInt();
    m_prefix = (VMID) in.readObject();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeInt(m_id);
    out.writeObject(m_prefix);
  }
}
