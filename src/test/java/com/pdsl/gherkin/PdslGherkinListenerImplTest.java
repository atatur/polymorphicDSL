package com.pdsl.gherkin;

import com.pdsl.gherkin.models.GherkinStep;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

public class PdslGherkinListenerImplTest {

    @Test
    public void setStepContentAndKeyword_noComment_setsContentAndKeywordCorrectly() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);
        
        when(mockNode.getText()).thenReturn("Given some step text");
        when(mockNode.getSymbol()).thenReturn(null);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.GIVEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("Given some step text");
        assertThat(step.getStepType()).isEqualTo(GherkinStep.StepType.GIVEN);
        assertThat(step.getComments().isPresent()).isFalse();
        assertThat(step.getTags().isPresent()).isFalse();
    }

    @Test
    public void setStepContentAndKeyword_withInlineComment_extractsCommentCorrectly() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);

        when(mockNode.getText()).thenReturn("When some step text # some inline comment");
        when(mockNode.getSymbol()).thenReturn(null);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.WHEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("When some step text");
        assertThat(step.getStepType()).isEqualTo(GherkinStep.StepType.WHEN);
        assertThat(step.getComments().isPresent()).isTrue();
        assertThat(step.getComments().get()).containsExactly("some inline comment");
        assertThat(step.getTags().isPresent()).isFalse();
    }

    @Test
    public void setStepContentAndKeyword_withInlineTag_extractsTagCorrectly() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);

        when(mockNode.getText()).thenReturn("Then some step text #@tag1");
        when(mockNode.getSymbol()).thenReturn(null);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.THEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("Then some step text");
        assertThat(step.getStepType()).isEqualTo(GherkinStep.StepType.THEN);
        assertThat(step.getComments().isPresent()).isFalse();
        assertThat(step.getTags().isPresent()).isTrue();
        assertThat(step.getTags().get()).containsExactly("@tag1");
    }

    @Test
    public void setStepContentAndKeyword_withBothInlineAndBeforeTags_retainsAllDistinctly() {
        PdslGherkinListenerImpl listener = Mockito.spy(new PdslGherkinListenerImpl());
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);
        Token mockToken = Mockito.mock(Token.class);

        when(mockNode.getText()).thenReturn("Given some step text #@inlineTag");
        when(mockNode.getSymbol()).thenReturn(mockToken);
        Mockito.doReturn(java.util.List.of("@beforeTag", "beforeComment")).when(listener).getCommentsBefore(mockToken);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.GIVEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("Given some step text");
        assertThat(step.getTags().isPresent()).isTrue();
        assertThat(step.getTags().get()).containsExactly("@inlineTag", "@beforeTag");
        assertThat(step.getComments().isPresent()).isTrue();
        assertThat(step.getComments().get()).containsExactly("beforeComment");
    }
}
