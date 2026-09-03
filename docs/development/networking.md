# Networking wire format

How game nodes talk to each other, and — more importantly — **how to change what
they say without breaking players on an older build.**

Two clients that were compiled at different times routinely talk to each other:
a host on last month's release, a joiner on today's. The wire format is a
compatibility contract between those builds. This document is the "how do I *not*
break it" that the format needs.

## The format

Messages travel as a JSON envelope over the existing transport:

```json
{ "messageTypeId": "games.strategy.engine.display.IDisplay$NotifyDiceMessage",
  "payload": "{ ...typed fields... }" }
```

- `messageTypeId` — the discriminator; it is the payload class's fully-qualified
  name (`MessageType.of(Xyz.class)`), and it routes the payload to a typed
  handler. It replaces the old reflective op-code.
- `payload` — the message's fields, serialized as JSON by Gson.

A message type is a small class implementing `WebSocketMessage`, with a
`static final MessageType<X> TYPE`, a `toEnvelope()`, and a handler entry point.
See `IDisplay` and `IChatChannel` for live examples.

## Evolution recipe

The wire is a contract with **past and future builds**, not just the current one.
Gson deserialization is lenient — it drops JSON members with no matching field
and leaves absent fields at their default — and that leniency is exactly what
makes safe evolution possible, and unsafe evolution silent. Follow these rules.

### Do

- **Add fields as optional.** A new field is fine: old senders omit it and the
  receiver sees the default (`null`, `0`, `false`), so give every added field a
  sensible default and code that tolerates it. New senders write it; old
  receivers ignore it.
- **Tolerate unknown fields.** Never assume the payload contains only the fields
  this build knows about. A newer peer may add members; deserialization must not
  fail on them (Gson already ignores them — don't defeat that with strict custom
  parsing).
- **Store primitives.** Prefer `String`, `int`, `boolean`, and lists/maps of
  primitives over rich types and enums. Carry an enum as its `String` name and
  convert in the getter, so renaming the enum constant later is a one-line
  receiver change rather than a wire break. (This is the standing guidance on
  `WebSocketMessage`.)
- **Reference entities, don't ship them.** Units cross the wire as their stable
  UUID, and territories/players/resources by name, via `EntityRef`; the receiver
  re-resolves against its own `GameData`. Save-overlapping payloads (`Change`,
  delegate `saveState`, the bulk `GameData` transfer) ride as an `OpaqueBlob` —
  their existing Java-serialized bytes, base64'd inside the envelope — so the
  wire needs no second codec for them.

### Don't

- **Don't remove a field** other builds still send or read.
- **Don't rename a field.** A rename is a remove plus an add: old JSON carries the
  old name, which the renamed field no longer reads, so the value silently
  becomes the default. If you must rename in code, keep the wire name (e.g. with
  a Gson `@SerializedName`).
- **Don't retype a field** (e.g. `int` → `String`, scalar → object). Old payloads
  carry the old JSON type and stop deserializing correctly.
- **Don't reuse a field name** for a new meaning — same hazard as a retype, but
  quieter.
- **Don't reorder to a different shape.** Field *order* within a JSON object does
  not matter, but changing an object into an array (or vice versa) does.

### Breaking on purpose

Sometimes a change genuinely cannot be additive. Break deliberately and visibly:
introduce a **new, separately-named message type** (e.g. `SelectCasualtiesV2`)
rather than mutating the existing one. Both types can coexist on the wire during
a transition; senders pick the version their peer understands, and the old type
is retired only once no supported build sends it. A new type means a new
`messageTypeId`, so there is no ambiguity about which shape a given envelope
carries.

## The compatibility fixture corpus

The rules above are enforced, not just documented, by
`WireCompatibilityFixtureTest`
(`game-app/game-core/src/test/java/games/strategy/engine/message/`).

**What it is.** Under
`game-app/game-core/src/test/resources/wire-compatibility/` sits a corpus of
stored example payloads — each one a snapshot of what some released build put on
the wire — plus a `manifest.txt` listing the required fixtures. The test
deserializes each fixture into its *current* class and re-serializes it, then
asserts the result is a **superset** of the stored JSON: every stored member must
still be reproduced. Adding a field leaves every old member intact (extra members
are ignored) → green. Removing, renaming, or retyping a field means a stored
member can no longer be reproduced → red. That is the "Don't" list above, turned
into a failing build.

It replaces the retired op-code CSV, whose only "fix" on failure was to edit the
CSV to match your change — so a compatibility break could be rubber-stamped
silently. Here the corpus is the source of truth: `manifest.txt` is the required
list, and retiring a guarantee means deleting a fixture file **and** a manifest
line — a visible, reviewable diff, not a silent edit.

### A fixture file

`wire-compatibility/fixtures/<Interface>-<Message>.json`:

```json
{
  "type": "games.strategy.engine.display.IDisplay$NotifyDiceMessage",
  "payload": { "...": "the payload exactly as it serializes today" }
}
```

- `type` — the payload class's fully-qualified name (nested classes use `$`),
  i.e. the same string as its `messageTypeId`.
- `payload` — the JSON the current code produces for a representative instance.

### Adding a fixture for a new message type

When a later stage converts a method to a typed message, add one fixture so the
new type is guarded from then on:

1. Serialize a representative instance with a plain `new Gson()` (the same Gson
   the envelope uses) and copy the JSON — or hand-write it to match the fields.
2. Save it as `wire-compatibility/fixtures/<Interface>-<Message>.json` with the
   `type` / `payload` shape above.
3. Add the file name to `wire-compatibility/manifest.txt`.
4. Run
   `./gradlew :game-core:test --tests "games.strategy.engine.message.WireCompatibilityFixtureTest"`;
   a green run confirms the fixture matches the current serialization and is now
   pinned.

Any non-`WebSocketMessage` wire value type (e.g. `EntityRef`, `OpaqueBlob`) is
added the same way — the harness only needs a class name and a payload.

### Regenerating or retiring a fixture

Once committed, a fixture is a promise about a *past* build's bytes; do not
casually rewrite it to make a failing test pass, because that is precisely the
silent break the corpus exists to catch. Change a fixture only to deliberately
retire a compatibility guarantee (delete the file and its manifest line together)
or to introduce a new versioned type alongside the old one (add a fixture, keep
the old).
