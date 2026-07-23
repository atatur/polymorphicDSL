package com.pdsl.gherkin;

import com.pdsl.gherkin.models.GherkinStep;
import com.pdsl.gherkin.parser.GherkinLexer;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.mockito.Mockito;

import java.util.List;

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
    }

    @Test
    public void setStepContentAndKeyword_withInlineComment_doesNotExtractComment() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);

        when(mockNode.getText()).thenReturn("When some step text # some inline comment");
        when(mockNode.getSymbol()).thenReturn(null);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.WHEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("When some step text # some inline comment");
        assertThat(step.getStepType()).isEqualTo(GherkinStep.StepType.WHEN);
        assertThat(step.getComments().isPresent()).isFalse();
    }

    @Test
    public void setStepContentAndKeyword_withInlineTag_doesNotExtractTag() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);

        when(mockNode.getText()).thenReturn("Then some step text #@tag1 @tag2");
        when(mockNode.getSymbol()).thenReturn(null);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.THEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("Then some step text #@tag1 @tag2");
        assertThat(step.getStepType()).isEqualTo(GherkinStep.StepType.THEN);
        assertThat(step.getComments().isPresent()).isFalse();
    }

    @Test
    public void setStepContentAndKeyword_withBothInlineAndBeforeTags_doesNotExtractInlineTag() {
        PdslGherkinListenerImpl listener = Mockito.spy(new PdslGherkinListenerImpl());
        GherkinStep.Builder builder = new GherkinStep.Builder();
        TerminalNode mockNode = Mockito.mock(TerminalNode.class);
        Token mockToken = Mockito.mock(Token.class);

        when(mockNode.getText()).thenReturn("Given some step text #@inlineTag");
        when(mockNode.getSymbol()).thenReturn(mockToken);
        Mockito.doReturn(List.of("@beforeTag", "beforeComment")).when(listener).getCommentsBefore(mockToken);

        listener.setStepContentAndKeyword(builder, GherkinStep.StepType.GIVEN, mockNode);
        GherkinStep step = builder.build();

        assertThat(step.getStepContent().getRawString()).isEqualTo("Given some step text #@inlineTag");
        assertThat(step.getComments().isPresent()).isTrue();
        assertThat(step.getComments().get()).containsExactly("@beforeTag", "beforeComment");
    }

    @Test
    public void getCommentsBefore_withNullToken_returnsEmptyList() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        List<String> comments = listener.getCommentsBefore(null);
        assertThat(comments).isEmpty();
    }

    @Test
    public void getCommentsBefore_withEmptyTokenStream_returnsEmptyList() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        Token mockToken = Mockito.mock(Token.class);
        when(mockToken.getTokenIndex()).thenReturn(5);
        List<String> comments = listener.getCommentsBefore(mockToken);
        assertThat(comments).isEmpty();
    }

    @Test
    public void getCommentsBefore_withCommentsBeforeToken_extractsCommentsCorrectly() {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        CommonTokenStream mockTokenStream = Mockito.mock(CommonTokenStream.class);
        listener.setTokenStream(mockTokenStream);

        Token stepToken = Mockito.mock(Token.class);
        when(stepToken.getTokenIndex()).thenReturn(3);

        Token commentToken2 = Mockito.mock(Token.class);
        when(commentToken2.getChannel()).thenReturn(Token.HIDDEN_CHANNEL);
        when(commentToken2.getType()).thenReturn(GherkinLexer.COMMENT);
        when(commentToken2.getText()).thenReturn("# second comment");

        Token commentToken1 = Mockito.mock(Token.class);
        when(commentToken1.getChannel()).thenReturn(Token.HIDDEN_CHANNEL);
        when(commentToken1.getType()).thenReturn(GherkinLexer.COMMENT);
        when(commentToken1.getText()).thenReturn("# first comment");

        Token otherHiddenToken = Mockito.mock(Token.class);
        when(otherHiddenToken.getChannel()).thenReturn(Token.HIDDEN_CHANNEL);
        when(otherHiddenToken.getType()).thenReturn(GherkinLexer.TAGS);

        when(mockTokenStream.get(2)).thenReturn(commentToken2);
        when(mockTokenStream.get(1)).thenReturn(otherHiddenToken);
        when(mockTokenStream.get(0)).thenReturn(commentToken1);

        List<String> comments = listener.getCommentsBefore(stepToken);
        assertThat(comments).containsExactly("first comment", "second comment");
    }

    private static Stream<Arguments> channelStopProvider() {
        return Stream.of(
            Arguments.of(Token.DEFAULT_CHANNEL, List.of("comment after divider")),
            Arguments.of(Token.HIDDEN_CHANNEL, List.of("comment before divider", "comment after divider"))
        );
    }

    @ParameterizedTest
    @MethodSource("channelStopProvider")
    public void getCommentsBefore_channelDividerBehavior(int dividerChannel, List<String> expectedComments) {
        PdslGherkinListenerImpl listener = new PdslGherkinListenerImpl();
        CommonTokenStream mockTokenStream = Mockito.mock(CommonTokenStream.class);
        listener.setTokenStream(mockTokenStream);

        Token stepToken = Mockito.mock(Token.class);
        when(stepToken.getTokenIndex()).thenReturn(3);

        Token commentTokenBeforeDivider = Mockito.mock(Token.class);
        when(commentTokenBeforeDivider.getChannel()).thenReturn(Token.HIDDEN_CHANNEL);
        when(commentTokenBeforeDivider.getType()).thenReturn(GherkinLexer.COMMENT);
        when(commentTokenBeforeDivider.getText()).thenReturn("# comment before divider");

        Token dividerToken = Mockito.mock(Token.class);
        when(dividerToken.getChannel()).thenReturn(dividerChannel);
        when(dividerToken.getType()).thenReturn(GherkinLexer.TAGS);

        Token commentTokenAfterDivider = Mockito.mock(Token.class);
        when(commentTokenAfterDivider.getChannel()).thenReturn(Token.HIDDEN_CHANNEL);
        when(commentTokenAfterDivider.getType()).thenReturn(GherkinLexer.COMMENT);
        when(commentTokenAfterDivider.getText()).thenReturn("# comment after divider");

        when(mockTokenStream.get(2)).thenReturn(commentTokenAfterDivider);
        when(mockTokenStream.get(1)).thenReturn(dividerToken);
        when(mockTokenStream.get(0)).thenReturn(commentTokenBeforeDivider);

        List<String> comments = listener.getCommentsBefore(stepToken);
        assertThat(comments).containsExactlyElementsIn(expectedComments);
    }
}
