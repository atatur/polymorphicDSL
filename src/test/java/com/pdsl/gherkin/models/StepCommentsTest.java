package com.pdsl.gherkin.models;

import com.pdsl.gherkin.PdslGherkinInterpreterImpl;
import com.pdsl.gherkin.PdslGherkinListenerImpl;
import com.pdsl.gherkin.PdslGherkinRecognizer;
import com.pdsl.gherkin.DefaultGherkinTestSpecificationFactory;
import com.pdsl.grammars.AllGrammarsLexer;
import com.pdsl.grammars.AllGrammarsParser;
import com.pdsl.specifications.TestSpecification;
import com.pdsl.testcases.PreorderTestCaseFactory;
import com.pdsl.testcases.SingleTestOutputPreorderTestCaseFactory;
import com.pdsl.testcases.TestCase;
import com.pdsl.transformers.DefaultPolymorphicDslPhraseFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class StepCommentsTest {
    private static final String RESOURCE_PATH = "src/test/resources/framework_specifications/features/gherkin/";
    private static final String FILE_NAME = "step_comments.feature";
    private static final PdslGherkinRecognizer TRANSFORMER = new PdslGherkinInterpreterImpl();
    private static final PdslGherkinListenerImpl LISTENER = new PdslGherkinListenerImpl();
    private static final DefaultGherkinTestSpecificationFactory SPEC_FACTORY =
            new DefaultGherkinTestSpecificationFactory.Builder(
            new DefaultPolymorphicDslPhraseFilter(AllGrammarsParser.class, AllGrammarsLexer.class)).build();
    private static List<GherkinStep> steps;
    private static Collection<TestSpecification> specifications;

    @BeforeAll
    public static void setUp() throws IOException {
        final URI featureFile = Path.of(RESOURCE_PATH + FILE_NAME).toUri();
        Optional<GherkinFeature> featureOptional =
                TRANSFORMER.interpretGherkinFile(featureFile, LISTENER);
        assertThat(featureOptional.isPresent()).isTrue();
        GherkinFeature feature = featureOptional.get();
        specifications = SPEC_FACTORY.getTestSpecifications(Set.of(featureFile))
                .orElseThrow();
        steps = feature.getOptionalGherkinScenarios().get().getFirst().getStepsList().get();
        assertThat(steps).hasSize(4);
    }

    @Test
    public void precedingComments_parsedAndStoredCorrectly() {
        GherkinStep firstStep = steps.getFirst();
        assertThat(firstStep.getStepContent().getRawString().trim()).isEqualTo("When First step");
        assertThat(firstStep.getComments().isPresent()).isTrue();
        assertThat(firstStep.getComments().get())
                .containsExactly("my_tag1 my_tag_2", "before line comment First");
    }

    @Test
    public void lineWithDashComment_parsedAndStoredCorrectly() {
        GherkinStep secondStep = steps.get(1);
        assertThat(secondStep.getStepContent().getRawString().trim())
                .isEqualTo("Then Second step #inline comment for Second");
        assertThat(secondStep.getComments().isPresent()).isTrue();
        assertThat(secondStep.getComments().get())
                .containsExactly("before comment for Second", "before comment#2 for Second");
    }

    @Test
    public void noComment_parsedAndStoredCorrectly() {
        GherkinStep thirdStep = steps.get(2);
        assertThat(thirdStep.getStepContent().getRawString().trim()).isEqualTo("But Third step");
        assertThat(thirdStep.getComments().isPresent()).isFalse();
    }

    @Test
    public void inlineValueWithDash_parsedAndStoredCorrectly() {
        GherkinStep fourthStep = steps.get(3);
        assertThat(fourthStep.getStepContent().getRawString().trim())
                .isEqualTo("And Fourth step with \"#value\"");
        assertThat(fourthStep.getComments().isPresent()).isTrue();
        assertThat(fourthStep.getComments().get()).containsExactly("before line comment for Fourth");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void stepComments_propagatedToTestCaseMetadata_correctly() {
        Collection<TestCase> testCases = new PreorderTestCaseFactory().processTestSpecification(specifications);
        assertThat(testCases).hasSize(1);
        TestCase testCase = testCases.iterator().next();

        assertThat(testCase.getMetadata()).containsKey(TestCase.STEP_COMMENTS);

        Map<Integer, List<String>> allComments = (Map<Integer, List<String>>) testCase.getMetadata().get(TestCase.STEP_COMMENTS);
        assertThat(allComments).hasSize(3);
        assertThat(allComments).doesNotContainKey(2);

        assertThat(allComments.get(0))
                .containsExactly("my_tag1 my_tag_2", "before line comment First");
        assertThat(allComments.get(1))
                .containsExactly("before comment for Second", "before comment#2 for Second");
        assertThat(allComments.get(2)).isNull();
        assertThat(allComments.get(3))
                .containsExactly("before line comment for Fourth");
    }
}
