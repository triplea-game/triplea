# Java Conventions
- Follow: [Google java style](https://google.github.io/styleguide/javaguide.html)
- Generally follow: [Effective Java](http://thefinestartist.com/effective-java)
- Install and use IDE checkstyle and formatting, see the IDE setup notes in wiki.

## Avoid returning 'null', prefer returning an Optional value

**Good**
```
Optional<Value> method() {
  return Optional.empty();
}
```

**Avoid**
```
Value method() {
  return null;
}
```

## Avoid boolean method parameters

Avoid passing `boolean` values as parameters to public APIs. Instead create
overloaded methods, or use factory methods, or an enum parameter instead.

**Good**
```
// create method overloads instead of having a boolean parameter
void goodMethod() { }
void badMethod() { }

// Use an enum parameter
public int doComputation(CombatMode.ATTACK);
public int doComputation(CombatMode.RETREAT);

// Use a factory method rather than a constructor with a boolean param
MyObject.newValidating();
MyObject.newNonValidating();
```

**Avoid**
```
void method(boolean isGood) { }
public int doComputation(boolean isAttack);

void foo() {
  boolean validating = true;
  new MyObject(validating);
}
```

## Avoid Optional method parameters
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

## Annotate methods returning null with `@Nullable`

- If `null` is unavoidable, annotate nullable values with `@Nullable`

**Good**
```
@Nullable Value method() {
  return null;
}
```

**Avoid**
```
Value method() {
  return null;
}
```

## Depth first method ordering

AKA: "step-down-order"

> A caller function should always reside above the callee function...
>
> The Stepdown Rule tells us that we should organize our code so that we can read the code top-to-bottom"

For more details:
- https://dzone.com/articles/the-stepdown-rule
- [Clean Code: Chapter 5 'Formatting'](http://ricardogeek.com/docs/clean_code.html)

**Good**
```java

public void firstPublicMethod() {
  firstPrivateMethodInvoked();
  secondPrivateMethodInvoked();
}

private void firstPrivateMethodInvoked() { }

public void secondPublicMethod() {
  secondPrivateMethodInvoked();
}

private void secondPrivateMethodInvoked() { }
```

**Avoid**
```java

public void firstPublicMethod() {
  firstPrivateMethodInvoked();
  secondPrivateMethodInvoked();
}

public void secondPublicMethod() {
  secondPrivateMethodInvoked();
}

// all private methods at bottom (bad!)
private void firstPrivateMethodInvoked() { }

private void secondPrivateMethodInvoked() { }
```

**Avoid**
```java

// method defined above where it is used (bad!)
private void someMethod() { }

public void publicMethod() {
  someMethod();
}
```

Note:
 - checkstyle will require method overloads to be declared next to each other

## Define Variables Close to their usage

Define variables as close to their usage as possible.

**Good**
```
void method() {
  int a = 123;
  functionCall(a);
  int b = 999;
  functionCall(b);
}
```

**Avoid**
```
void method() {
  int a = 123;
  int b = 999;
  functionCall(a);
  functionCall(b);
}
```

## Favor constructor injection, avoid setters [#5489](https://github.com/triplea-game/triplea/issues/5489)
**Good**
```
class Foo {
  private final int a;

  Foo(int a) {
    this.a = a;
  }
}
```

**Avoid**
```
class Foo {
  private int a;

  Foo() { }

  void setA(int a) {
    this.a = a;
  }
}
```

## Avoid Mutability

Avoid mutating variables whenever possible.

**Good**
```
int value = condition ? 3 : 2;
```

**Avoid**

```
int value = 2;
if(condition) {
  value = 3;
}
```

## Do not mutate parameters, avoid "out" and avoid "in/out" parameters [#5489](https://github.com/triplea-game/triplea/issues/5489)

**Good**
```
Collection<String> processCollection(Collection<String> input) {
  Collection<String> copy = new ArrayList<>(input);
  copy.add("new value");
  return copy;
}
```

**Avoid**
```
void processCollection(Collection<String> input) {
  input.add("new value");
}
```

## TODO comments: attach a tracking 'token' [#5249](https://github.com/triplea-game/triplea/issues/5249)

Attach a 'tracking token', or a 'grep token' to TODO comments so that they can all be found as part of larger piece of
work. Favor TODO statements that are a clear in terms of what needs to be done.

**Good**
```
TODO: Project#12 ... this relates to a github project
TODO: Issue#5312 ... this relates to an initiative discussed in github issues
TODO: Topic#1183 ... this relates to a forum discussion
```

## Import inner classes

**Good**
```
import someClass.innerClass;

innerClass.methodCall();
```

**Avoid**
```
someClass.innerClass.methodCall();
```

## Refer to objects by their interfaces [#5489](https://github.com/triplea-game/triplea/issues/5489)

Effective Java Item 52: <http://thefinestartist.com/effective-java/52>

**Good**
```
Collection<String> strings = new ArrayList<>();
```

**Avoid**
```
ArrayList<String> strings = new ArrayList<>();
```

##  Use most restrictive scope possible [#5489](https://github.com/triplea-game/triplea/issues/5489)

Use the most restrictive modifier available that still allows the code to compile, ie (in this order):

  - private
  - package-default
  - protected
  - public

## Mark values and methods as static where possible [#5489](https://github.com/triplea-game/triplea/issues/5489)

This helps mostly for reading comprehension and to highlight static dependency anti-pattern, eg:

**Good**
```
private static void thisDoesNotUseClassState(GuaranteedByTheCompiler arg) {}
```

**Avoid**
```
private void thisDoesNotUseClassState(NoCompilerGuarantee arg) { }
```

##  Avoid 'static coupling' [#5489](https://github.com/triplea-game/triplea/issues/5489)

The idea is to avoid using static dependencies. A common way to avoid static dependencies is to use interfaces.

**Good**
```
public constructor(Supplier<Integer> compute) {
  myVar = compute.get();
}
```

**Avoid**
```
public constructor() {
  myVar = StaticCall.compute();
}
```

### Avoid boolean property values [#5489](https://github.com/triplea-game/triplea/issues/5489)

**Avoid**
Instead of:
```
visible: false
isAttack: false
```

**Good**
```
visibility: visible
combatType: attack
```

### Favor using `List.of()` for empty lists [#5524](https://github.com/triplea-game/triplea/issues/5524)

Prefer to use `List.of` over:
```
Collections.singletonList
Collections.emptyList
Collections.unmodifiableList
Arrays.asList
```

Same with `Map.of` and `Set.of`

### Guidelines for `var` usage (https://github.com/triplea-game/triplea/issues/6290)

The following is a good guide: <https://openjdk.java.net/projects/amber/LVTIstyle.html>

> Omitting an explicit type can reduce clutter, but only if its omission doesn't impair understandability.
> The type isn't the only way to convey information to the reader. Other means include the variable's name and the
> initializer expression. We should take all the available channels into account when determining whether it's OK to
> mute one of these channels.

In short, recommending we favor `var` when:
- variable is partially or fully redundant to the type
- hiding ugly nested generic types (which perhaps are better created into a data type object, though var is a potential
  tool in our toolbelt)

**Good**
```
var diceRoll = new DiceRoll(..)
var territoryImage = new Image(...)
var unitCount = unitsLeft() - unitsHit();
```

**Avoid**
```
var hits = new DiceRoll(...)
var power = new  HashMap<Unit, Integer>();
```

#### Collections

Plural names on collections can be fine. EG:

```
var units = new ArrayList<Unit>();
```

If the underlying 'non-var' version is clearly a collection already, the var variant is likely okay. Map types should be
considered a bit more as often maps need to be carefully named. For example:

```
var units = new HashMap<unit, Integer>();  // << not well named
```

Something that describes the key to value relationship is generally going to be more clear:
```
var unitHitpoints = new HashMap<unit, Integer>();
```

