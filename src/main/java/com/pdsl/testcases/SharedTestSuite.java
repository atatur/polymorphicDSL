package com.pdsl.testcases;

import com.google.common.base.Preconditions;
import com.pdsl.executors.InterpreterObj;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SharedTestSuite {

  public List<SharedTestCase> getSharedTestCaseList() {
    return sharedTestCaseList;
  }

  private List<SharedTestCase> sharedTestCaseList;


  /**
   * A record that encapsulates the data required to create a {@link SharedTestSuite}.
   *
   * <p>This record serves as a data transfer object (DTO) that groups together the test cases and their
   * corresponding interpreters. It also performs crucial validation in its compact constructor to ensure
   * the integrity of the test data before it is processed further.
   *
   * <p>The validation rules are as follows:
   * <ul>
   *   <li>The number of inner lists in {@code testCaseList} must be equal to the number of interpreters
   *       in {@code interpreterObj}. This ensures that each interpreter has a corresponding list of test cases.
   *   <li>All inner lists in {@code testCaseList} must have the same size. This is necessary because the
   *       framework collates the test cases by index across the different interpreter-specific lists.
   * </ul>
   *
   * @param testCaseList A list of lists of {@link TestCase}s. Each inner list corresponds to a specific
   *                     interpreter and contains the test cases for that interpreter.
   * @param interpreterObj A list of {@link InterpreterObj}s that will be used to execute the test cases.
   */
  public record SharedTestCaseData(List<List<TestCase>> testCaseList, List<InterpreterObj> interpreterObj) {

      public SharedTestCaseData {
          /*
          One test case per interpreter
           */
          int interpreterSize = interpreterObj.size();
          /*
          every interpreter should have its own test suite
           */
          Preconditions.checkArgument(testCaseList.size() == interpreterSize);
          /*
          every test suite should have the same number of test cases
          * */
          testCaseList.forEach(
                  item -> Preconditions.checkArgument(item.size() == testCaseList.get(0).size()));
      }

  }

  private SharedTestSuite(List<SharedTestCase> sharedTestCasesList) {
    this.sharedTestCaseList = sharedTestCasesList;
  }

  public static SharedTestSuite of(List<SharedTestCaseData> testCaseDataList) {
    /*
    every test suite should have the same number of test cases
    * */
    return new SharedTestSuite(testCaseDataList.stream()
            .map(testCaseData -> collateTestCasesWithInterpreters(testCaseData.testCaseList(), testCaseData.interpreterObj()))
            .flatMap(Collection::stream)
            .map(SharedTestCase::new)
            .toList());
  }

  public static SharedTestSuite of(List<List<TestCase>> listOfTestCases,
      List<InterpreterObj> interpreterObj) {
    return of(List.of(new SharedTestCaseData(listOfTestCases, interpreterObj)));
  }

  private static @NonNull List<List<SharedTestCaseWithInterpreter>> collateTestCasesWithInterpreters(List<List<TestCase>> listOfTestCases,
                                                                                                     List<InterpreterObj> interpreterObj) {
    int numTestCases = listOfTestCases.get(0).size();
    int numInterpreters = interpreterObj.size();
    List<List<SharedTestCaseWithInterpreter>> result = new ArrayList<>(numTestCases);
    for (int testCaseIndex = 0; testCaseIndex < numTestCases; testCaseIndex++) {
      List<SharedTestCaseWithInterpreter> innerList = new ArrayList<>(numInterpreters);
      for (int interpreterIndex = 0; interpreterIndex < numInterpreters; interpreterIndex++) {
        innerList.add(new SharedTestCaseWithInterpreter(
            listOfTestCases.get(interpreterIndex).get(testCaseIndex),
            interpreterObj.get(interpreterIndex)));
      }
      result.add(innerList);
    }
    return result;
  }

  public static class SharedTestCaseWithInterpreter {

    private final InterpreterObj interpreterObj;
    private final TestCase testCase;

    SharedTestCaseWithInterpreter(TestCase testCase, InterpreterObj interpreterObj) {
      this.interpreterObj = interpreterObj;
      this.testCase = testCase;
    }

    public InterpreterObj getInterpreterObj() {
      return interpreterObj;
    }

    public TestCase getTestCase() {
      return testCase;
    }
  }

}