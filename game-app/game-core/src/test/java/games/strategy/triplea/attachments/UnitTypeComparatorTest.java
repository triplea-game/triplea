package games.strategy.triplea.attachments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.UnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitTypeComparatorTest {

  private final UnitTypeComparator unitTypeComparator = new UnitTypeComparator();

  @Mock private UnitType nullType;
  @Mock private UnitType infrastructure;
  @Mock private UnitType antiAircraft;
  @Mock private UnitType air;
  @Mock private UnitType sea;
  @Mock private UnitType attacker;
  @Mock private UnitType noneType1;
  @Mock private UnitType noneType2;

  @BeforeEach
  void setUp() {
    when(nullType.getName()).thenReturn("<NULL TYPE>");

    final var infrastructureAttachment = mock(UnitAttachment.class);
    when(infrastructureAttachment.isInfrastructure()).thenReturn(true);
    when(infrastructure.getAttachment(any())).thenReturn(infrastructureAttachment);
    when(infrastructure.getName()).thenReturn("<Infrastructure TYPE>");

    final var antiAircraftAttachment = mock(UnitAttachment.class);
    when(antiAircraftAttachment.isAaForBombingThisUnitOnly()).thenReturn(true);
    when(antiAircraft.getAttachment(any())).thenReturn(antiAircraftAttachment);
    when(antiAircraft.getName()).thenReturn("<AA TYPE>");

    final var airAttachment = mock(UnitAttachment.class);
    when(airAttachment.isAir()).thenReturn(true);
    when(air.getAttachment(any())).thenReturn(airAttachment);
    when(air.getName()).thenReturn("<Air TYPE>");

    final var seaAttachment = mock(UnitAttachment.class);
    when(seaAttachment.isSea()).thenReturn(true);
    when(sea.getAttachment(any())).thenReturn(seaAttachment);
    when(sea.getName()).thenReturn("<Sea Type>");

    final var attackerAttachment = mock(UnitAttachment.class);
    when(attackerAttachment.getAttack()).thenReturn(1);
    when(attacker.getAttachment(any())).thenReturn(attackerAttachment);
    when(attacker.getName()).thenReturn("<Attacker Type>");

    when(noneType1.getAttachment(any())).thenReturn(mock(UnitAttachment.class));
    when(noneType1.getName()).thenReturn("<NONE TYPE> - 1");

    when(noneType2.getAttachment(any())).thenReturn(mock(UnitAttachment.class));
    when(noneType2.getName()).thenReturn("<NONE TYPE> - 0");
  }

  @Test
  void testUnitTypeComparator() {
    assertThrows(
        IllegalStateException.class, () -> unitTypeComparator.compare(nullType, noneType1));
    assertThrows(
        IllegalStateException.class, () -> unitTypeComparator.compare(noneType1, nullType));
    assertThrows(IllegalStateException.class, () -> unitTypeComparator.compare(nullType, nullType));

    assertThat(unitTypeComparator.compare(infrastructure, infrastructure), is(0));
    assertThat(unitTypeComparator.compare(antiAircraft, antiAircraft), is(0));
    assertThat(unitTypeComparator.compare(air, air), is(0));
    assertThat(unitTypeComparator.compare(sea, sea), is(0));
    assertThat(unitTypeComparator.compare(attacker, attacker), is(0));
    assertThat(unitTypeComparator.compare(noneType1, noneType1), is(0));

    assertThat(unitTypeComparator.compare(infrastructure, antiAircraft), is(1));
    assertThat(unitTypeComparator.compare(antiAircraft, air), is(1));
    assertThat(unitTypeComparator.compare(air, sea), is(1));
    assertThat(unitTypeComparator.compare(sea, attacker), is(1));
    assertThat(unitTypeComparator.compare(attacker, noneType1), is(1));
    assertThat(unitTypeComparator.compare(noneType1, noneType2), is(1));

    assertThat(unitTypeComparator.compare(antiAircraft, infrastructure), is(-1));
    assertThat(unitTypeComparator.compare(air, antiAircraft), is(-1));
    assertThat(unitTypeComparator.compare(sea, air), is(-1));
    assertThat(unitTypeComparator.compare(attacker, sea), is(-1));
    assertThat(unitTypeComparator.compare(noneType1, attacker), is(-1));
    assertThat(unitTypeComparator.compare(noneType2, noneType1), is(-1));
  }
}
