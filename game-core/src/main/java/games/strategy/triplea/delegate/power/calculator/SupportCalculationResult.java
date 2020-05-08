package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

@Builder
@Value
public class SupportCalculationResult {
  public static final SupportCalculationResult EMPTY_RESULT =
      SupportCalculationResult.builder()
          .supportRules(new HashSet<>())
          .supportUnits(new HashMap<>())
          .supportLeft(new IntegerMap<>())
          .build();

  Set<List<UnitSupportAttachment>> supportRules;
  IntegerMap<UnitSupportAttachment> supportLeft;
  Map<UnitSupportAttachment, IntegerMap<Unit>> supportUnits;
}
