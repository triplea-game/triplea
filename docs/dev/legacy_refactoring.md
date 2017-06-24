---
layout: longpage
title: Legacy Refactoring
permalink: /dev_docs/dev/legacy_refactoring/
---

This page lists examples of existing code conventions/patterns that we are trying to update
to be more useful. 

## Comments


* Many comments documents "what" the code does, but not "why". Look to removing the "what" comments by improving 
the structure of the code so the "what" is much more obvious. Then add and keep the comments that describe "why"
something is happening.

## Repeated Code

* There are lots of examples of copy/pasted code. Particularly with Java8 lambdas, we can express much of the
same code at a higher level without the needless redundancy.


## Non-Isolated Testing

* Testing happens far too often at a high level. For example, if we set up a full WWII game to then test artillery support,
that is a whole lot of effort given the number of permutations:
- countries
- round, whether abilities are activated
- unit counts
- types of units
- amphib assault?
- blitz attack?

Let's say for each case we only have 2 options, it's 2^6 test cases! If we add anything else, then we have to double our
test cases. At this pace we won't be able to add new code and maintain effective test coverage. Instead if we break
up that logic into modules, we can make it an additive problem.

## Too Many and Unnecessary Class Variables

Class variables represent class state. In general state is not a very good thing, and we shoudl favor immutable states
whenever possible since state is easier to understand/reason about when it is immutable (and automatic thread safety! oh yeah)

Ways to fix this is to see if we can move the class variable to the methods that use it. Often there is only one method.
See about extracting a variable and surrounding methods to a new class/module.
