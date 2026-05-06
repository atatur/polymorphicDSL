package com.pdsl.runners;

/**
  A rule that describes the logic around running phrases in a shared test case where multiple interpreters may be used on the same test.

  By default if a phrase is recognized by an interpreter it runs it.

  The intent of these contraints is to reduce surprising behavior in projects requiring more fine grained coordination and to have these constraints
  technically enforced.
*/
public enum InterpreterConstraint {

    NONE,  // If a phrase is recognized by 1 or more interpreter they all run it (material implication)
    NO_DUPLICATES_PHRASES // Each phrase can be recognized by 0 or 1 among all the interpreters (mutual exclusion)
}
