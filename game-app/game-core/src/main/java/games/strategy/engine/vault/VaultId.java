package games.strategy.engine.vault;

import games.strategy.net.INode;
import java.io.Serializable;
import lombok.EqualsAndHashCode;

/** Uniquely identifies a cryptographic vault used to store random numbers on a particular node. */
@EqualsAndHashCode
public class VaultId implements Serializable {
  private static final long serialVersionUID = 8863728184933393296L;
  private static long currentId;

  private final INode generatedOn;
  // this is a unique and monotone increasing id
  // unique in this vm
  private final long uniqueId = getNextId();

  VaultId(final INode generatedOn) {
    this.generatedOn = generatedOn;
  }

  private static synchronized long getNextId() {
    return currentId++;
  }

  INode getGeneratedOn() {
    return generatedOn;
  }

  @Override
  public String toString() {
    return "VaultId generated on: " + generatedOn + " id: " + uniqueId;
  }
}
