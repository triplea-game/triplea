# Java Conventions
- Follow: [Google java style](http://google.github.io/styleguide/javaguide.html)
- Install and use IDE checkstyle and formatting, see the IDE setup notes in wiki.

## `null` handling

### Prefer Optional Return Values

Avoid returning `null` values. Prefer returning `Optional`

**Good**
```
Optional<Value> method() {
```

**Avoid**
```
Value method() {
```

### As boolean parameters - Prefer Multiple Method Signatures

Avoid passing `null` values as parameters to public APIs. Prefer method overloads so the parameter does not need to be specified at all

**Good**
```
void goodMethod() { }
void badMethod() { }
```

**Avoid**
```
void method(boolean isGood) { }
```

### Avoid optional parameters
- Avoid `Optional` parameters, prefer method overloads that omit parameters

**Good**
```
void goodMethod() { }
void goodMethod(int value) { }
```

**Avoid**
```
void method(Optional<Integer> value) { }
```

### Annotate methods returning null with `@Nullable`

- If `null` is unavoidable, annotate nullable values with `@Nullable`

**Good**
```
@Nullable Value method() { }
```

**Avoid**
```
Value method() { }
```

## Depth first method ordering
For full details, please see *Chapter 5 'Formatting'* in [Clean Code](http://ricardogeek.com/docs/clean_code.html)

Example:
```java

public void firstPublicMethod() {
  firstPrivateMethodInvoked();
  secondPrivateMethodInvoked();
}

private void firstPrivateMethodInvoked() { }
private void secondPrivateMethodInvoked() { }

public void secondPublicMethod() { }
```

Note:
 - goal is to keep vertical distance between first usage and declaration reasonably short / minimized.
 - to the extent possible, allow a reader to read code in a single vertical pass from top to bottom
   without having to jump around to find method definitions.
 - checkstyle will require method overloads to be declared next to each other

## Variables
Define variables as close to their usage as possible.

## Favor constructor injection, avoid setters [#5489](https://github.com/triplea-game/triplea/issues/5489)

## Avoid Mutability

Try to make all variables final and immutable.

Instead of:
```
int value = 2;
if(condition) {
  value = 3;
}
```

Prefer:
```
final int value = condition ? 3 : 2;
```

## Do not mutate parameters, avoid "out" and avoid "in/out" parameters [#5489](https://github.com/triplea-game/triplea/issues/5489)

Instead of:
```
void processCollection(Collection<String> input) {
  input.add("new value");
}
```

Favor:
```
Collection<String> processCollection(Collection<String> input) {
  Collection<String> copy = new ArrayList<>(input);
  copy.add("new value");
  return copy;
}

```

## TODO comments: attach a tracking 'token' [#5249](https://github.com/triplea-game/triplea/issues/5249)

Attach a 'tracking token', or a 'grep token' to TODO comments so that they can all be found as part of larger piece of work. Favor TODO statements that are a clear in terms of what needs to be done.

EG:
```
TODO: Project#12 ... this relates to a github project
TODO: Issue#5312 ... this relates to an initiative discussed in github issues
TODO: Topic#1183 ... this relates to a forum discussion
```

## Import inner classes

Instead of:
```
someClass.innerClass.methodCall();
```
Favor:

```
import someClass.innerClass;

innerClass.methodCall();
```

## Refer to objects by their interfaces [#5489](https://github.com/triplea-game/triplea/issues/5489)

Effective Java Item 52: http://thefinestartist.com/effective-java/52

Instead of:
```
ArrayList<String> strings = new ArrayList<>();
```
Favor:
```
Collection<String> strings = new ArrayList<>();
```

## Avoid boolean parameters on public methods [#5489](https://github.com/triplea-game/triplea/issues/5489)

Prefer APIs that do not accept a boolean parameter, use factory methods, enums, or break up such an API to have the 'true'/'false' variants.

Instead of:
```
public int doComputation(boolean isAttack);
```

Prefer:
```

```
Or Prefer:
```
public int doComputation(CombatMode.ATTACK);
public int doComputation(CombatMode.RETREAT);
```

Instead of:
```
boolean validating = true;
new MyObject(true);
```

Prefer:
```
MyObject.newValidating();
MyObject.newNonValidating();
```

##  Use most restrictive scope possible [#5489](https://github.com/triplea-game/triplea/issues/5489)

Use the most restrictive modifier available that still allows the code to compile, ie (in this order):

  - private
  - package-default
  - protected
  - public

## Mark values and methods as static where possible [#5489](https://github.com/triplea-game/triplea/issues/5489)

This helps mostly for reading comprehension and to highlight static dependency anti-pattern, eg:

Instead of:
```
private void thisDoesNotUseClassState(NoCompilerGuarantee arg) { }
```
Prefer:
```
private static void thisDoesNotUseClassState(GuaranteedByTheCompiler arg) {}
```

##  Avoid 'static coupling' [#5489](https://github.com/triplea-game/triplea/issues/5489)

The idea is to avoid using static dependencies. A common way to avoid static dependencies is to use interfaces.
Instead of:
```
public constructor() {
  myVar = StaticCall.compute();
}
```
Prefer:
```
public constructor(Supplier<Integer> compute) {
  myVar = compute.get();
}
```

### Avoid boolean property values [#5489](https://github.com/triplea-game/triplea/issues/5489)

Instead of:
```
visible: true
visible: false
```

Favor:
```
visibility: visible
visibility: hidden
```

Instead of:
```
isAttack: false
```

Favor:
```
combat: attack
```

### Favor `List.of` [#5524](https://github.com/triplea-game/triplea/issues/5524)

Prefer to use `List.of` over:
```
Collections.singletonList
Collections.emptyList
Collections.unmodifiableList
Arrays.asList
```

Same with `Map.of` and `Set.of`

### Guidelines for `var` usage (https://github.com/triplea-game/triplea/issues/6290)

The following is a really good guide: https://openjdk.java.net/projects/amber/LVTIstyle.html

> Omitting an explicit type can reduce clutter, but only if its omission doesn't impair understandability. The type isn't the only way to convey information to the reader. Other means include the variable's name and the initializer expression. We should take all the available channels into account when determining whether it's OK to mute one of these channels.

In short, recommending we favor `var` when:
- variable is partially or fully redundant to the type
- hiding ugly nested generic types (which perhaps are better created into a data type object, though var is a potential tool in our toolbet)

#### Good
```
var diceRoll = new DiceRoll(..)
var territoryImage = new Image(...)
var unitCount = unitsLeft() - unitsHit();
```

#### Avoid
```
var hits = new DiceRoll(...)
var power = new  HashMap<Unit, Integer>();
```

#### Collections

Plural names on collections can be fine. EG:

```
var units = new ArrayList<Unit>();
```

If the underlying 'non-var' version is clearly a collection already, the var variant is likely okay. Map types should be considered a bit more as often maps need to be carefully named. For example:

```
var units = new HashMap<unit, Integer>();  // << not well named
```

Something that describes the key to value relationship is generally going to be more clear:
```
var unitHitpoints = new HashMap<unit, Integer>();
```

