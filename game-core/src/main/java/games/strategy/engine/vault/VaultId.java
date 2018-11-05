package games.strategy.engine.vault;

import java.io.Serializable;
import java.util.Objects;

import games.strategy.net.INode;

/**
 * Uniquely identifies a cryptographic vault used to store random numbers on a particular node.
 */
public class VaultId implements Serializable {
  private static final long serialVersionUID = 8863728184933393296L;
  private static long currentId;

  private static synchronized long getNextId() {
    return currentId++;
  }

  private final INode generatedOn;
  // this is a unique and monotone increasing id
  // unique in this vm
  private final long uniqueId = getNextId();

  VaultId(final INode generatedOn) {
    this.generatedOn = generatedOn;
  }

  INode getGeneratedOn() {
    return generatedOn;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof VaultId)) {
      return false;
    }
    final VaultId other = (VaultId) o;
    return other.generatedOn.equals(this.generatedOn) && other.uniqueId == this.uniqueId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uniqueId, generatedOn.getName());
  }

  @Override
  public String toString() {
    return "VaultId generated on:" + generatedOn + " id:" + uniqueId;
  }
}
