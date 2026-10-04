---
paths:
  - "game-app/**"
  - "http-clients/**"
---

# Compatibility rules (save games and network)

Breaking these silently breaks every existing save game or every mixed-version online game.

- Classes extending `GameDataComponent` (and everything they serialize) are read by Java
  serialization. NEVER rename, delete or retype their private fields, and NEVER move such a
  class to another package. Adding a field is allowed; it must tolerate being `null`/default
  when an old save is loaded.
- Methods annotated `@RemoteActionCode` (remote delegate interfaces such as `IBattleDelegate`,
  `IMoveDelegate`) are called by method number over the network. NEVER change their signature
  or their `@RemoteActionCode` number; a unit test checks this.
- Game state changes go through `Change` objects from `ChangeFactory`, applied with
  `DelegateBridge.addChange()`. NEVER mutate `GameData` directly from a delegate; it breaks
  network sync and history.
- Map XML is community content: a rule or attachment change MUST keep existing map XMLs
  loading and behaving the same unless the user asked for a rule change.
- If a change cannot avoid breaking one of the above, stop and say so explicitly before editing.
