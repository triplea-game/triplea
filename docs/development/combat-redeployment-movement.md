# Combat and redeployment movement

Small Front unit types can define different movement allowances for the combat and after-combat movement phases.

```xml
<attachment name="unitAttachment" attachTo="infantry">
  <option name="movement" value="1"/>
  <option name="combatMovement" value="1"/>
  <option name="redeploymentMovement" value="3"/>
</attachment>
```

Both phase-specific values are optional non-negative integers. If omitted, the legacy `movement` value is used. Existing technology and local bonus-movement modifiers apply after the phase value is selected.

`Unit.getMaxMovementAllowed()` and `Unit.getMovementLeft()` use the current game step, so movement validators, route searches, UI selection, aircraft landing checks, transports, and AI utilities that consume unit movement share the same allowance.

Air units retain movement spent during combat and may use any remaining redeployment allowance. At the end of Combat Move, every non-air unit that moved is marked beyond its redeployment allowance. This preserves the rule that non-air units used during Combat Move cannot move again during After Combat Move, even when their redeployment allowance is larger.
