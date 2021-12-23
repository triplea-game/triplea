* use bash, start scripts with `!#/bin/bash`
* prefer to start scripts with `set -eu` to terminate on first failure and terminate on undefined variables
* for inner script evaluation, prefer `$()` notation over backtick ``` ` ```
* Run shellcheck on shell scripts and fix any issues flagged
* Use subshell when using the `cd` command. When the subshell exits, the original directory is restored, eg:
 ```
   readonly PATH1=$(pwd)
   (cd directory; ./do_something)
   readonly PATH2=$(pwd)
   #PATH1 and PATH2 will be equal
```
* variables declared in an outermost scope get CAPS
* use 'readonly' whenever possible for outermost variables
* use 'local -r' for variables defined in functions (-r is for readonly)
* local variables are lowerCamelCased
* Break up long scripts into functions with a main method, eg:

```

function main() {
  function1
  function2
}

function1() {
  :
  :
}

function2() {
  :
  :
}

main
```

* Asssign function parameters to variables in the first lines of a function. EG:
```
function foo() {
   local -r firstVariable="$1"
   echo "Now reference first parameter by name: $firstVariable"
}
```
