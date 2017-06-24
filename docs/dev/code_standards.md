---
layout: longpage
title: Java Code Guidelines and Standards
permalink: /dev_docs/dev/code_standards/
---

## Checkstyle

This project uses Checkstyle to enforce code guidelines and standards. See the [Checkstyle]({{ "/dev_docs/dev/checkstyle/" | prepend: site.baseurl }}) page for information on setting up and running Checkstyle in your development environment.

## Code is formatted

According to [Code Format]({{ "/dev_docs/dev/code_format/" | prepend: site.baseurl }})

## Tests included for any new code

- New (Non-AI) code should keep the current code coverage rate, not decrease it
- Follow the [testing pyramid](https://martinfowler.com/bliki/TestPyramid.html), write mostly unit tests
- Tests are difficult in the tripleA project for the existing code. Any significant changes will likely require a few iterations of simplification and test retrofits before any new  code can be added. There is some leniency for fixing/refactoring existing code cruft without retrofitting tests, there is a a lot less for new code, or major updates.


## DRY

Do not repeat yourself: Basically [do not copy and paste](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself), consolidate redundant code

## Spell out variable names

***Prefer***
(note the variable names)
<pre><code>void foo(Unit unit, Attachment attachment, int count) {
 if( count > 0 ) ...
 :
}</code></pre>

***notable exceptions***
<pre><code>Exception e
for(int i; ...</code></pre>

***Avoid***
<pre><code>void foo(Unit u, Attachment a, int b) {
 if( b > 0 ) ...
 :
}</code></pre>


## Consistent Naming
- Write TripleA with a capitol 'A'
- Acronyms are in caps, for examaple "UIContext"
- Stay with google java naming convention, please see: [google naming convention](http://google.github.io/styleguide/javaguide.html#s5-naming), e.g: camelCase, no underscore or 'm_' prefixes, immutable objects are in caps

## Prefer library implementations when available (do not re-invent the wheel)
- First use a `java.lang` feature if there is one to accomplish your task
- Failing that we have guava, apache commons, and mockito that have a lot of library functions

## Avoid using nulls
Use empty collection types (empty set, empty list), or Optional instead.

## Deprecate Correctly
When you have a good reason to deprecate a method or class, add both - a @Deprecated annotation _and_ a @deprecated documentation in the javadocs

## Handle exceptions

- `ClientLogger` is a utility class to log errors. Can be used as: `ClientLogger.logError(e)`
- Always log surrounding context, if there are any args or relevant variable values, log them, for example:

<pre><code>public int processXmlData(Unit unit, XmlTransmitter transmitter) {
  try {
     transmitter.send(unit)
  } catch (IOException e ) {
     String failMsg = String.format("Failed to send unit data: %s, using transmitter: %s", unitData, transmitter);
     ClientLogger.logError(failMsg, e);
  }
}</code></pre>

In the above, note that we are logging the values of the two method arguments. If there were any other interesting variable values in the method or class, we would log those too. Without this information, if we ever do get an exception, and it is related to data, we'll be scratching our heads on how to reproduce the problem.

## Method and variable ordering
Try to organize methods and variables so that new elements are used immediately and only *after* they are declared. This basically attempts to allow for code to be read from top to bottom once. For some good background reading and details on how to do this, please see Chapter 5 'Formatting' in [Clean Code](http://ricardogeek.com/docs/clean_code.html)


### Variable Ordering Example

#### Prefer
<pre><code>int first = 2;
int firstSquared = first * first;

int second = 3;
int secondSquared = second * second;

double distance = Math.sqrt(firstSquared + secondSquared);</code></pre>

#### Avoid

<pre><code>int first = 2;
int second = 3;

int firstSquared = first * first;
int secondSquared = second * second;

double distance = Math.sqrt(firstSquared + secondSquared);</code></pre>

### Method Ordering Example

#### Prefer

<pre><code>  // constructor is listed first
  public constructor() {
     int a = helperMethod1();
     boolean b = helperMethod2(a);
  }
  // helperMethod1 is the first thing used in the constructor, so we start by defining that first.
  private int helperMethod1() {
    :  // any methods called by helperMethod1 would be defined next
    :  // in this example we just do generic processing, so next we define helperMethod2 since that was next
  }
  private boolean helperMethod2() {
    :
    :
  }
  // now when we see the first public method, we know all the private methods above it until the constructor are there
  // only to support construction. Note that we gain now quite a bit of information based simply on where methods are places
  public boolean firstPublicMethod1() {
     :
     :
     helperMethod3();
     helperMethod4();
     :
  }
  private void helperMethod3() {
     helperMethod5();
     :
  }
  private void helperMethod5() { // We'll fully define the code path that followed helperMethod3() first
      :                          // before we define helperMethod4()
      :
  }
  private void helperMethod4() {
      :
      :
  }</code></pre>

### Avoid

<pre><code>  // private method that is defined above the first method that uses it
  private int helperMethod1() {
    :
    :
  }
  public constructor() {
     int a = helperMethod1();
     boolean b = helperMethod2(a);
  }
  // skipped definition of 'helperMethod1' and 'helperMethod2'
  public boolean firstPublicMethod1() {
     :
     :
     helperMethod3();
     helperMethod4();
     :
  }
  // all private methods grouped at bottom of file.
  // There is at least some structure here, but for java files that are much longer that quickly breaks down.
  // For example, at 500, or 1000+ lines, all of the private methods grouped togeter can become a jumble.
  private boolean helperMethod2() {
    :
    :
  }
  // no consistent ordering here, private methods are at least grouped, but there is no
  // consistent greater structure here.
  private void helperMethod3() {
     helperMethod5();
     :
  }
  private void helperMethod5() {
      :
  }
  private void helperMethod4() {
      :
      :
  }</code></pre>


## Mock Objects

Prefer replacing hand crafted mock objects with mockito.
