## Java Code Guidelines for Pull Requests

### Use google code conventions when there is disagreement on format etc..
- When in doubt, follow: https://google.github.io/styleguide/javaguide.html

### Please Use the auto-code formatter
- https://github.com/triplea-game/triplea/wiki/Code-Format


### DRY - do not repeat yourself
- https://en.wikipedia.org/wiki/Don%27t_repeat_yourself
Avoid copy/paste code, repetitious code.

### Spell out variable names
Instead of:
```
void foo(Unit u, Attachment a, int b) {
 if( b > 0 ) ...
 :
}
```

Prefer:
```
void foo(Unit unit, Attachment attachment, int count) {
 if( count > 0 ) ...
 :
}
```

*notable exceptions*
- `Exception e`
- `for(int i`

###Use consistent Naming

- Write TripleA with a capitol 'A'
- Stay with the Java Code Standard, e.g: variable names lowercase, constants uppercase, class names start with a capitol letter etc.


### Prefer to use library implementations when available (do not re-invent the wheel)
- First use a `java.lang` feature if there is one to accomplish your task
- Failing that we have guava, apache commons, mockito that have a lot of library functions

### Avoid using nulls
Use empty collection types (empty set, empty list), or Optional instead

### Deprecate Correctly
When you have a good reason to derecate a method or complete class, add both - a @Deprecated annotation _and_ a @deprecated documentation in the javadocs

### Handle exceptions completely
- `ClientLogger` is a utility class to log errors. Can be used as: `ClientLogger.logError(e)`
- Always log surrounding context, if there are any args or relevant values, log them, for example:
```
public int processXmlData(Unit unit, XmlTransmitter transmitter) {
  try{
     transmitter.send(unit)
  } catch (IOException e ) {
     String failMsg = "Failed to send unit data: " + unit + ", using transmitter: " + transmitter;
     ClientLogger.logError(failMsg, e);
  }
}

```
In the above, note that we are logging the values of the two arg, if there were any interesting intermediate values in the method, we would log those too. Without this information, if we ever do get an exception, and it is related to data, we'll be scratching our heads on how to reproduce the problem. 

