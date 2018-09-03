
# Code Standards

Unless specified otherwise, follow: [Google java style](http://google.github.io/styleguide/javaguide.html)

Project uses checkstyle which must pass for any code to be merged.

## [Checkstyle](http://checkstyle.sourceforge.net)

Build will fail if checkstyle violation is increased.

New sub-projects should enforce checkstyle and allow no violations.


## Guidelines and Preferences

### Avoid using `null` as part of public APIs

Said another way, do not pass `null` arguments, and do not return null. 

### Deprecate Correctly
To deprecate add both a `@Deprecated` annotation  _and_ a `@deprecated` documentation 
in the javadocs. Include a comment on what can be done to avoid the deprecated call.

Example:
```
/**
 * @Deprecated Use 'fooBar()' instead
 */
 @deprecated
 public void foo(int param) {
 :
 :
```


## Variable and Method Ordering

Depth first ordering for methods according to call stack.
For more details, please see Chapter 5 'Formatting' in [Clean Code](http://ricardogeek.com/docs/clean_code.html)

Example:
```java

public void method1() {
  callPrivate1();
  callPrivate2();
}

private void callPrivate1() { }
private void callPrivate2() { }

public void method2() { }
```

Note:
 - vertical distance between first usage and declaration is minimized. 
 - checkstyle will require method overloads to be declared next to each other, that
  is an exception enforced by checkstyle.


## Variables
Define variables as close to their usage as possible.

