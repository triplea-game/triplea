package games.strategy.triplea.delegate.data;

import com.google.common.collect.ImmutableList;
import games.strategy.engine.data.Unit;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Getter;

/**
 * The result of validating a unit movement action. For an invalid movement action, provides details
 * on which units are not permitted to perform the movement.
 */
public class MoveValidationResult implements Serializable, Comparable<MoveValidationResult> {
  private static final long serialVersionUID = 6648363112533514955L;
  @Getter private String error = null;
  private final List<String> disallowedUnitWarnings;
  private final List<Collection<Unit>> disallowedUnitsList;
  private final List<String> unresolvedUnitWarnings;
  private final List<Collection<Unit>> unresolvedUnitsList;

  public MoveValidationResult() {
    disallowedUnitWarnings = new ArrayList<>();
    disallowedUnitsList = new ArrayList<>();
    unresolvedUnitWarnings = new ArrayList<>();
    unresolvedUnitsList = new ArrayList<>();
  }

  public void addDisallowedUnit(final String warning, final Unit unit) {
    int index = disallowedUnitWarnings.indexOf(warning);
    if (index == -1) {
      index = disallowedUnitWarnings.size();
      disallowedUnitWarnings.add(warning);
      disallowedUnitsList.add(new ArrayList<>());
    }
    final Collection<Unit> disallowedUnits = disallowedUnitsList.get(index);
    disallowedUnits.add(unit);
  }

  public void addUnresolvedUnit(final String warning, final Unit unit) {
    int index = unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      index = unresolvedUnitWarnings.size();
      unresolvedUnitWarnings.add(warning);
      unresolvedUnitsList.add(new ArrayList<>());
    }
    final Collection<Unit> unresolvedUnits = unresolvedUnitsList.get(index);
    unresolvedUnits.add(unit);
  }

  /**
   * Removes the specified unit from the list of unresolved units associated with the specified
   * warning.
   */
  public void removeUnresolvedUnit(final String warning, final Unit unit) {
    final int index = unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      return;
    }
    final Collection<Unit> unresolvedUnits = unresolvedUnitsList.get(index);
    if (!unresolvedUnits.remove(unit)) {
      return;
    }
    if (unresolvedUnits.isEmpty()) {
      unresolvedUnitsList.remove(unresolvedUnits);
      unresolvedUnitWarnings.remove(warning);
    }
  }

  public void setError(final String error) {
    this.error = error;
  }

  public MoveValidationResult setErrorReturnResult(final String error) {
    this.error = error;
    return this;
  }

  public Collection<Unit> getUnresolvedUnits(final String warning) {
    final int index = unresolvedUnitWarnings.indexOf(warning);
    if (index == -1) {
      return List.of();
    }
    return ImmutableList.copyOf(unresolvedUnitsList.get(index));
  }

  public String getDisallowedUnitWarning(final int index) {
    if (index < 0 || index >= disallowedUnitWarnings.size()) {
      return null;
    }
    return disallowedUnitWarnings.get(index);
  }

  public String getUnresolvedUnitWarning(final int index) {
    if (index < 0 || index >= unresolvedUnitWarnings.size()) {
      return null;
    }
    return unresolvedUnitWarnings.get(index);
  }

  public boolean hasError() {
    return error != null;
  }

  public boolean hasDisallowedUnits() {
    return !disallowedUnitWarnings.isEmpty();
  }

  public int getDisallowedUnitCount() {
    return disallowedUnitWarnings.size();
  }

  public boolean hasUnresolvedUnits() {
    return !unresolvedUnitWarnings.isEmpty();
  }

  public int getUnresolvedUnitCount() {
    return unresolvedUnitWarnings.size();
  }

  public boolean isMoveValid() {
    return !hasError() && !hasDisallowedUnits() && !hasUnresolvedUnits();
  }

  public int getTotalWarningCount() {
    return unresolvedUnitWarnings.size() + disallowedUnitWarnings.size();
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
    return Integer.compare(getUnresolvedUnitCount(), other.getUnresolvedUnitCount());
  }

  @Override
  public String toString() {
    return "Move Validation Results, error:" + error + " isValid():" + isMoveValid();
  }
}
