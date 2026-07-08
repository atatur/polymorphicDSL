package com.pdsl.testcases;

import com.pdsl.executors.InterpreterObj;
import com.pdsl.specifications.FilteredPhrase;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedTestSuiteTest {

    private static final InterpreterObj DUMMY_INTERPRETER_1 = new InterpreterObj(new DummyListener());
    private static final InterpreterObj DUMMY_INTERPRETER_2 = new InterpreterObj(new DummyListener());

    private static final TestCase DUMMY_TEST_CASE_1 = new DummyTestCase("Test Case 1");
    private static final TestCase DUMMY_TEST_CASE_2 = new DummyTestCase("Test Case 2");

    @Test
    void of_unevenLists_throwsException() {
        List<List<TestCase>> testCases = List.of(
                List.of(DUMMY_TEST_CASE_1),
                List.of(DUMMY_TEST_CASE_1, DUMMY_TEST_CASE_2)
        );
        List<InterpreterObj> interpreters = List.of(DUMMY_INTERPRETER_1);
        assertThrows(IllegalArgumentException.class, ()->SharedTestSuite.of(testCases, interpreters));
    }

    @Test
    void of_validInput_createsSharedTestSuite() {
        List<List<TestCase>> testCases = List.of(
                List.of(DUMMY_TEST_CASE_1, DUMMY_TEST_CASE_2),
                List.of(DUMMY_TEST_CASE_1, DUMMY_TEST_CASE_2)
        );
        List<InterpreterObj> interpreters = List.of(DUMMY_INTERPRETER_1, DUMMY_INTERPRETER_2);

        SharedTestSuite sharedTestSuite = SharedTestSuite.of(testCases, interpreters);

        assertThat(sharedTestSuite.getSharedTestCaseList()).hasSize(2);
        SharedTestCase sharedTestCase1 = sharedTestSuite.getSharedTestCaseList().get(0);
        assertThat(sharedTestCase1.getSharedTestCaseWithInterpreters()).hasSize(2);
        assertThat(sharedTestCase1.getSharedTestCaseWithInterpreters().get(0).getTestCase()).isEqualTo(DUMMY_TEST_CASE_1);
        assertThat(sharedTestCase1.getSharedTestCaseWithInterpreters().get(0).getInterpreterObj()).isEqualTo(DUMMY_INTERPRETER_1);

        SharedTestCase sharedTestCase2 = sharedTestSuite.getSharedTestCaseList().get(1);
        assertThat(sharedTestSuite.getSharedTestCaseList()).hasSize(2);
        assertThat(sharedTestCase2.getSharedTestCaseWithInterpreters().get(0).getTestCase()).isEqualTo(DUMMY_TEST_CASE_2);
        assertThat(sharedTestCase2.getSharedTestCaseWithInterpreters().get(0).getInterpreterObj()).isEqualTo(DUMMY_INTERPRETER_1);
        assertThat(sharedTestCase2.getSharedTestCaseWithInterpreters().get(1).getTestCase()).isEqualTo(DUMMY_TEST_CASE_2);
        assertThat(sharedTestCase2.getSharedTestCaseWithInterpreters().get(1).getInterpreterObj()).isEqualTo(DUMMY_INTERPRETER_2);
    }

    private record DummyTestCase(String name) implements TestCase {

        @Override
            public URI getOriginalSource() {
                return null;
            }

            @Override
            public List<String> getUnfilteredPhraseBody() {
                return null;
            }

            @Override
            public List<String> getContextFilteredPhraseBody() {
                return null;
            }

            @Override
            public String getTestTitle() {
                return "";
            }

            @Override
            public Iterator<TestSection> getContextFilteredTestSectionIterator() {
                return null;
            }

            @Override
            public List<FilteredPhrase> getFilteredPhrases() {
                return List.of();
            }

            @Override
            public Map<String, Object> getMetadata() {
                return null;
            }
        }

    private static class DummyListener implements ParseTreeListener {
        @Override
        public void visitTerminal(TerminalNode node) {

        }

        @Override
        public void visitErrorNode(ErrorNode node) {

        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {

        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {

        }
    }
}
