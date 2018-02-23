package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import games.strategy.engine.data.Unit;

public class MoveValidationResult implements Serializable, Comparable<MoveValidationResult> {
  private static final long serialVersionUID = 6648363112533514955L;
  private String m_error = null;
  private final List<String> m_disallowedUnitWarnings;
  private final List<Collection<Unit>> m_disallowedUnitsList;
  private final List<String> m_unresolvedUnitWarnings;
  private final List<Collection<Unit>> m_unresolvedUnitsList;

  public MoveValidationResult() {
    m_disallowedUnitWarnings = new ArrayList<>();
    m_disallowedUnitsList = new ArrayList<>();
    m_unresolvedUnitWarnings = new ArrayList<>();
    m_unresolvedUnitsList = new ArrayList<>();
  }

  public void addDisallowedUnit(final String warning, final Unit unit) {
    int index = m_disallowedUnitWarnings.indexOf(warning);
    if (index == -1) {
      index = m_disallowedUnitWarnings.size();
      m_disallowedUnitWarnings.add(warning);
      m_disallowedUnitsList.add(new ArrayList<>());
    }
    final Collection<Unit> disallowedUnits = m_disallowedUnitsList.get(index);
    disallowedUnits.add(unit);
  }

  public void addUnresolvedUnit(final String warning, final Unit unit) {
    int index = m_unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      index = m_unresolvedUnitWarnings.size();
      m_unresolvedUnitWarnings.add(warning);
      m_unresolvedUnitsList.add(new ArrayList<>());
    }
    final Collection<Unit> unresolvedUnits = m_unresolvedUnitsList.get(index);
    unresolvedUnits.add(unit);
  }

  public boolean removeUnresolvedUnit(final String warning, final Unit unit) {
    final int index = m_unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      return false;
    }
    final Collection<Unit> unresolvedUnits = m_unresolvedUnitsList.get(index);
    if (!unresolvedUnits.remove(unit)) {
      return false;
    }
    if (unresolvedUnits.isEmpty()) {
      m_unresolvedUnitsList.remove(unresolvedUnits);
      m_unresolvedUnitWarnings.remove(warning);
    }
    return true;
  }

  public void setError(final String error) {
    m_error = error;
  }

  public MoveValidationResult setErrorReturnResult(final String error) {
    m_error = error;
    return this;
  }

  public String getError() {
    return m_error;
  }

  public Collection<Unit> getUnresolvedUnits(final String warning) {
    final int index = m_unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      return Collections.emptyList();
    }
    return new ArrayList<>(m_unresolvedUnitsList.get(index));
  }

  public Collection<String> getDisallowedUnitWarnings() {
    return new ArrayList<>(m_disallowedUnitWarnings);
  }

  public Collection<String> getUnresolvedUnitWarnings() {
    return new ArrayList<>(m_unresolvedUnitWarnings);
  }

  public String getDisallowedUnitWarning(final int index) {
    if ((index < 0) || (index >= m_disallowedUnitWarnings.size())) {
      return null;
    }
    return m_disallowedUnitWarnings.get(index);
  }

  public String getUnresolvedUnitWarning(final int index) {
    if ((index < 0) || (index >= m_unresolvedUnitWarnings.size())) {
      return null;
    }
    return m_unresolvedUnitWarnings.get(index);
  }

  public boolean hasError() {
    return m_error != null;
  }

  public boolean hasDisallowedUnits() {
    return m_disallowedUnitWarnings.size() > 0;
  }

  public int getDisallowedUnitCount() {
    return m_disallowedUnitWarnings.size();
  }

  public boolean hasUnresolvedUnits() {
    return m_unresolvedUnitWarnings.size() > 0;
  }

  public int getUnresolvedUnitCount() {
    return m_unresolvedUnitWarnings.size();
  }

  public boolean isMoveValid() {
    return !hasError() && !hasDisallowedUnits() && !hasUnresolvedUnits();
  }

  public int getTotalWarningCount() {
    return m_unresolvedUnitWarnings.size() + m_disallowedUnitWarnings.size();
  }

  @Override
  public int compareTo(final MoveValidationResult other) {
    if (!hasError() && other.hasError()) {
      return -1;
    }
    if (hasError() && !other.hasError()) {
      return 1;
    }
    if (getDisallowedUnitCount() < other.getDisallowedUnitCount()) {
      return -1;
    }
    if (getDisallowedUnitCount() > other.getDisallowedUnitCount()) {
      return 1;
    }
    if (getUnresolvedUnitCount() < other.getUnresolvedUnitCount()) {
      return -1;
    }
    if (getUnresolvedUnitCount() > other.getUnresolvedUnitCount()) {
      return 1;
    }
    return 0;
  }

  @Override
  public String toString() {
    return "Move Validation Results, error:" + m_error + " isValid():" + isMoveValid();
  }
}
