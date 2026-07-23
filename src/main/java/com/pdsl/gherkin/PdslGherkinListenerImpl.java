package com.pdsl.gherkin;

import com.pdsl.gherkin.models.*;
import com.pdsl.gherkin.parser.GherkinLexer;
import com.pdsl.gherkin.parser.GherkinParser;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.net.URI;
import java.util.*;

public class PdslGherkinListenerImpl extends PdslGherkinListener {

    private static final String GHERKIN_TAG_PREFIX = "@";
    private static final Set<Character> ESCAPE_CHARACTERS = Set.of('\\', '|', 'n');

    private Optional<GherkinFeature.Builder> builderOptional = Optional.empty();

    private Optional<CommonTokenStream> tokens = Optional.empty();

    @Override
    public Optional<GherkinFeature> getGherkinFeature(URI featurePathOrId) {
        return builderOptional.map(builder -> builder.withLocation(featurePathOrId).build());
    }

    @Override
    public void setTokenStream(CommonTokenStream tokens) {
        this.tokens = Optional.ofNullable(tokens);
    }

    @Override
    public void enterGherkinDocument(GherkinParser.GherkinDocumentContext ctx) {
        if (ctx.feature() != null) {
            GherkinFeature.Builder builder = new GherkinFeature.Builder();
            builderOptional = Optional.of(builder);
            // Get the language code
            if (ctx.LANGUAGE_HEADER() != null) {
                String languageCode = ctx.LANGUAGE_HEADER().getText().split(":")[1].trim();
                builder.withLanguageCode(languageCode);
            }
        }
    }

    @Override
    public void enterFeature(GherkinParser.FeatureContext ctx) {
        assert (builderOptional.isPresent()) : "GherkinFeature builder not initialized!";
        GherkinFeature.Builder builder = builderOptional.get();
        if (ctx.TAGS() != null) {
            builder.withTags(terminalNodes2StringList(ctx.TAGS()));
        }
        if (ctx.FEATURE_TITLE() != null) {
            builder.withTitle(ctx.FEATURE_TITLE().getText());
        }
        if (ctx.LONG_DESCRIPTION() != null) {
            builder.withLongDescription(list2String(ctx.LONG_DESCRIPTION()));
        }
        if (ctx.background() != null) {
            builder.withBackground(transformBackground(ctx.background()));
        }
        if (ctx.scenario() != null) {
            ctx.scenario().forEach(s -> builder.addScenario(transformScenario(s)));
        }
        if (ctx.ruleBlock() != null) {
            ctx.ruleBlock().forEach(r -> builder.addRule(transformRule(r)));
        }
    }

    private GherkinRule transformRule(GherkinParser.RuleBlockContext ctx) {
        GherkinRule.Builder builder = new GherkinRule.Builder();
        if (ctx.RULE_TITLE() != null) {
            builder.withTitle(ctx.RULE_TITLE().getText());
        }
        if (ctx.LONG_DESCRIPTION() != null) {
            builder.withLongDescription(transformLongDescription(ctx.LONG_DESCRIPTION()));
        }
        if (ctx.background() != null) {
            builder.withGherkinBackground(transformBackground(ctx.background()));
        }
        if (ctx.scenario() != null) {
            ctx.scenario().forEach(s -> builder.addScenarios(transformScenario(s)));
        }
        return builder.build();
    }

    private GherkinScenario transformScenario(GherkinParser.ScenarioContext ctx) {
        GherkinScenario.Builder scenarioBuilder = new GherkinScenario.Builder();
        List<String> tags = new LinkedList<>();
        if (!ctx.TAGS().isEmpty()) {
            ctx.TAGS().forEach(t -> tags.add(t.getText()));
            scenarioBuilder.withTags(tags);
        }
        if (ctx.SCENARIO_TITLE() != null) {
            scenarioBuilder.withTitle(ctx.SCENARIO_TITLE().getText())
                    .withPosition(ctx.SCENARIO_TITLE().getSymbol().getLine());
        } else if (ctx.SCENARIO_OUTLINE_TITLE() != null) {
            scenarioBuilder.withTitle(ctx.SCENARIO_OUTLINE_TITLE().getText());
        }
        if (ctx.LONG_DESCRIPTION() != null) {
            scenarioBuilder.withLongDescription(transformLongDescription(ctx.LONG_DESCRIPTION()));
        }
        if (ctx.stepBody() != null) {
            scenarioBuilder.withSteps(transformStepBody(ctx.stepBody()));
        }
        if (ctx.examplesBody() != null) {
            ctx.examplesBody().forEach(e -> scenarioBuilder.addExamples(transformExamples(e)));
        }
        return scenarioBuilder.build();
    }

    private GherkinExamplesTable transformExamples(GherkinParser.ExamplesBodyContext ctx) {
        GherkinExamplesTable.Builder builder = new GherkinExamplesTable.Builder();
        if (ctx.EXAMPLES_TITLE() != null) {
            builder.withTitle(ctx.EXAMPLES_TITLE().getText());
        }
        if (ctx.TAGS() != null) {
            List<String> tags = new LinkedList<>();
            ctx.TAGS().forEach(t -> tags.add(t.getText()));
            builder.withTags(tags);
        }
        if (ctx.LONG_DESCRIPTION() != null) {
            builder.withLongDescription(transformLongDescription(ctx.LONG_DESCRIPTION()));
        }
        if (ctx.DATA_ROW() != null) {
            // Each element in the outer list represents a row
            // The inner list represents the cells in that row
            List<ExampleTableRow> cellsPerRow = new LinkedList<>();
            ctx.DATA_ROW().forEach(row -> cellsPerRow.add(new ExampleTableRow(row, transformRowData(row.getText()))));

            builder.withTable(createSubstitutionMapping(cellsPerRow));
        }
        return builder.build();
    }

    private record ExampleTableRow(TerminalNode originalSource, List<String> unescapedCellParameters) {
    }

    private Map<String, List<GherkinExamplesTable.CellOfExamplesTable>> createSubstitutionMapping(List<ExampleTableRow> rows) {
        Map<String, List<GherkinExamplesTable.CellOfExamplesTable>> substitutions = new HashMap<>();

        List<String> header = rows.getFirst().unescapedCellParameters;
        for (int i = 0; i < header.size(); i++) { // For each cell entry in a row
            List<GherkinExamplesTable.CellOfExamplesTable> parameters = new LinkedList<>(); // Create a list of all substitution data
            for (int j = 1; j < rows.size(); j++) { // Add the cells from the ith column in the table
                ExampleTableRow row = rows.get(j); // For each row
                parameters.add(new GherkinExamplesTable.CellOfExamplesTable(
                        row.originalSource.getSymbol().getLine(),
                        // Get the parameter from the column that matches the header we're iterating over
                        row.unescapedCellParameters.get(i).strip() // Keep escaped newlines, but ignore whitespace
                ));
            }
            substitutions.put("<" + header.get(i) + ">", parameters);
        }
        return substitutions;
    }

    private String transformLongDescription(List<TerminalNode> description) {
        StringBuilder builder = new StringBuilder();
        description.forEach(d -> builder.append(d.getText()));
        return builder.toString();
    }

    private List<GherkinStep> transformStepBody(GherkinParser.StepBodyContext ctx) {
        List<GherkinStep> steps = new LinkedList<>();
        if (ctx.startingStep() == null) {
            return steps;
        } else {
            GherkinStep.Builder stepBuilder = new GherkinStep.Builder();

            if (ctx.startingStep().GIVEN_STEP() != null) {
                setStepContentAndKeyword(stepBuilder, GherkinStep.StepType.GIVEN, ctx.startingStep().GIVEN_STEP());
            } else if (ctx.startingStep().WHEN_STEP() != null) {
                setStepContentAndKeyword(stepBuilder, GherkinStep.StepType.WHEN, ctx.startingStep().WHEN_STEP());
            } else if (ctx.startingStep().THEN_STEP() != null) {
                setStepContentAndKeyword(stepBuilder, GherkinStep.StepType.THEN, ctx.startingStep().THEN_STEP());
            } else if (ctx.startingStep().WILD_STEP() != null) {
                setStepContentAndKeyword(stepBuilder, GherkinStep.StepType.WILD, ctx.startingStep().WILD_STEP());
            } else {
                throw new IllegalArgumentException("Error creating a step");
            }

            // Docstring XOR Datatable
            if (ctx.startingStep().DOCSTRING() != null) {
                stepBuilder.withDocString(ctx.startingStep().DOCSTRING().getText());
            } else if (ctx.startingStep().DATA_ROW() != null) {
                List<List<GherkinString>> tableData = new LinkedList<>();
                ctx.startingStep().DATA_ROW().forEach(r -> tableData.add(transformRowData(r.getText())
                        .stream()
                        .map(GherkinString::new)
                        .toList()
                ));
                stepBuilder.withDataTable(tableData);
            }
            steps.add(stepBuilder.build());
            // all other steps
            for (GherkinParser.AnyStepContext stepCtx : ctx.anyStep()) {
                GherkinStep.Builder anyStepBuilder = new GherkinStep.Builder();
                if (stepCtx.GIVEN_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.GIVEN, stepCtx.GIVEN_STEP());
                } else if (stepCtx.WHEN_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.WHEN, stepCtx.WHEN_STEP());
                } else if (stepCtx.THEN_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.THEN, stepCtx.THEN_STEP());
                } else if (stepCtx.BUT_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.BUT, stepCtx.BUT_STEP());
                } else if (stepCtx.WILD_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.WILD, stepCtx.WILD_STEP());
                } else if (stepCtx.AND_STEP() != null) {
                    setStepContentAndKeyword(anyStepBuilder, GherkinStep.StepType.AND, stepCtx.AND_STEP());
                } else {
                    throw new IllegalArgumentException("Error creating a step");
                }

                // Docstring XOR Datatable
                if (stepCtx.DOCSTRING() != null) {
                    anyStepBuilder.withDocString(stepCtx.DOCSTRING().getText());
                } else if (stepCtx.DATA_ROW() != null) {
                    List<List<GherkinString>> tableData = new LinkedList<>();
                    stepCtx.DATA_ROW().forEach(r -> tableData.add(transformRowData(r.getText())
                            .stream()
                            .map(GherkinString::new)
                            .toList()
                    ));
                    anyStepBuilder.withDataTable(tableData);
                }
                steps.add(anyStepBuilder.build());
            }
        }
        return steps;
    }

    private GherkinBackground transformBackground(GherkinParser.BackgroundContext ctx) {
        GherkinBackground.Builder background = new GherkinBackground.Builder();
        if (ctx.BACKGROUND_TITLE() != null) {
            background.withTitle(ctx.BACKGROUND_TITLE().getText());
        }
        if (ctx.LONG_DESCRIPTION() != null) {
            background.withLongDescription(list2String(ctx.LONG_DESCRIPTION()));
        }
        if (ctx.stepBody() != null) {
            background.withSteps(transformStepBody(ctx.stepBody()));
        }
        return background.build();
    }

    private List<String> transformRowData(String rowText) {
        List<String> cellData = new LinkedList<>();
        StringBuilder cellText = new StringBuilder();
        boolean possibleEscapeCharacter = false;
        String row = rowText.trim();
        for (int i = 1; i < row.length(); i++) {
            char c = row.charAt(i);
            if (possibleEscapeCharacter) {
                if (ESCAPE_CHARACTERS.contains(c)) {
                    cellText.append(c == 'n' ? "\n" : c);
                } else { // False escape
                    cellText.append("\\").append(c);
                }
                possibleEscapeCharacter = false;
            } else {
                if (c == '\\') { // Escape character
                    possibleEscapeCharacter = true;
                } else if (c == '|') { // End of cell
                    cellData.add(cellText.toString() //trim() removes leading and trailing whitespace
                            .replaceAll("^[ \t]*", "") // Remove leading spaces and tabs
                            .replaceAll("[ \t]*$", "") // Remove trailing spaces and tabs
                    );
                    cellText = new StringBuilder();
                } else {
                    cellText.append(c);
                }
            }
        }
        return cellData;
    }

    private List<String> terminalNodes2StringList(List<TerminalNode> nodeList) {
        List<String> tags = new LinkedList<>();
        nodeList.forEach(t -> tags.add(t.getText()));
        return tags;
    }

    private String list2String(List<TerminalNode> nodeList) {
        StringBuilder stringBuilder = new StringBuilder();
        nodeList.forEach(s -> stringBuilder.append(s.getText()));
        return stringBuilder.toString();
    }

    protected List<String> getCommentsBefore(Token token) {
        if (token == null || tokens.isEmpty()) {
            return List.of();
        }
        int tokenIndex = token.getTokenIndex();
        List<String> comments = new ArrayList<>();
        for (int i = tokenIndex - 1; i >= 0; i--) {
            Token t = tokens.get().get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                break;
            }
            if (t.getType() == GherkinLexer.COMMENT) {
                // remove comment symbol in the beginning of line
                String clean = t.getText().trim().substring(1);
                if (!clean.isEmpty()) {
                    comments.addFirst(clean.trim());
                }
            }
        }
        return comments;
    }

    void setStepContentAndKeyword(GherkinStep.Builder stepBuilder, GherkinStep.StepType type,
                                  TerminalNode node) {
        String stepContent = node.getText();
        for (String comment : getCommentsBefore(node.getSymbol())) {
            processComments(comment, stepBuilder);
        }
        stepBuilder.withStepKeyword(type, stepContent)
                .withStepContent(stepContent);
    }

    private void processComments(String commentContent, GherkinStep.Builder stepBuilder) {
        if (commentContent == null || commentContent.isBlank()) {
            return;
        }
        int tagIndex = commentContent.indexOf(GHERKIN_TAG_PREFIX);
        if (tagIndex == -1) {
            stepBuilder.addComment(commentContent);
            return;
        }
        String commentPart = commentContent.substring(0, tagIndex).trim();
        if (!commentPart.isEmpty()) {
            stepBuilder.addComment(commentContent);
        }
        String[] tags = commentContent.substring(tagIndex + 1).split(GHERKIN_TAG_PREFIX);
        for (String tag : tags) {
            tag = tag.trim();
            if (!tag.isEmpty()) {
                stepBuilder.addComment(GHERKIN_TAG_PREFIX + tag);
            }
        }
    }
}
