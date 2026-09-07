package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): dependent semantics beyond a simple carries-N (eg per-cargo cost, air-on-carrier)
// are deferred to the allocator port.
/** What a carrier profile transports; consulted at casualty apply and battle end. */
public record CargoRule(UnitTypeId cargoType, int capacity) {}
