package games.strategy.engine.vault;

import java.io.Serializable;
import java.util.Objects;

import games.strategy.net.INode;

/**
 * Uniquely identifies a cryptographic vault used to store random numbers on a particular node.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName") // rename upon next incompatible release
public class VaultID implements Serializable {
  private static final long serialVersionUID = 8863728184933393296L;
  private static long currentId;

  private static synchronized long getNextId() {
    return currentId++;
  }

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final INode m_generatedOn;
  // this is a unique and monotone increasing id
  // unique in this vm
  @SuppressWarnings({
      "checkstyle:AbbreviationAsWordInName", // rename upon next incompatible release
      "checkstyle:MemberName" // rename upon next incompatible release
  })
  private final long m_uniqueID = getNextId();

  VaultID(final INode generatedOn) {
    m_generatedOn = generatedOn;
  }

  INode getGeneratedOn() {
    return m_generatedOn;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof VaultID)) {
      return false;
    }
    final VaultID other = (VaultID) o;
    return other.m_generatedOn.equals(this.m_generatedOn) && other.m_uniqueID == this.m_uniqueID;
  }

  @Override
  public int hashCode() {
    return Objects.hash(m_uniqueID, m_generatedOn.getName());
  }

  @Override
  public String toString() {
    return "VaultID generated on:" + m_generatedOn + " id:" + m_uniqueID;
  }
}
