package com.pdsl.gherkin.models;

import java.util.*;
import java.util.stream.Collectors;

public class GherkinStep {

    private final StepType stepType;
    private final Optional<GherkinDocString> docString;
    private final Optional<List<List<GherkinString>>> dataTable;
    private final Optional<Collection<String>> comments;
    private final String stepKeywordText;
    private final GherkinString stepContent;

    private GherkinStep(Builder builder) {
        Objects.requireNonNull(builder.stepKeywordText);
        Objects.requireNonNull(builder.stepContent);
        this.docString = builder.docString.isEmpty() ? Optional.empty()
                : Optional.of(new GherkinDocString(builder.docString));
        this.stepType = builder.stepType;
        this.dataTable = builder.dataTable.isEmpty() ? Optional.empty() : Optional.of(builder.dataTable);
        this.stepKeywordText = builder.stepKeywordText;
        this.stepContent = new GherkinString(builder.stepContent);
        this.comments = builder.comments.isEmpty() ? Optional.empty() : Optional.of(new ArrayList<>(builder.comments));
    }

    /**
     * Returns the Gherkin keyword type of this step (e.g., GIVEN, WHEN, THEN).
     *
     * @return the {@link StepType} of the step
     */
    public StepType getStepType() {
        return stepType;
    }

    /**
     * Returns the multi-line docstring attached to this step, if present.
     *
     * @return an {@code Optional} containing the {@link GherkinDocString},
     *         or {@code Optional.empty()} if there is no docstring
     */
    public Optional<GherkinDocString> getDocString() {
        return docString;
    }

    /**
     * Returns the 2D data table attached to this step, if present.
     *
     * @return an {@code Optional} containing a list of rows, where each row is a list of
     *         {@link GherkinString} cells, or {@code Optional.empty()} if there is no data table
     */
    public Optional<List<List<GherkinString>>> getDataTable() {
        return dataTable;
    }

    /**
     * Returns the raw Gherkin keyword text of this step (e.g., "Given", "When").
     *
     * @return the step keyword as a {@code String}
     */
    public String getStepKeywordText() {
        return stepKeywordText;
    }

    /**
     * Returns the text content of the step following the keyword.
     *
     * @return the {@link GherkinString} containing the step content
     */
    public GherkinString getStepContent() {
        return stepContent;
    }

    /**
     * Returns the comments associated with this step, if any.
     *
     * @return an {@code Optional} containing a list of comment strings associated with this step,
     *         or {@code Optional.empty()} if there are no comments
     */
    public Optional<Collection<String>> getComments() {
        return comments;
    }

    /**
     * Returns the text content of this step including the docstring xor datatable if present
     *
     * @return
     */
    public String getFullRawStepText() {
        StringBuilder str = new StringBuilder(getStepContent().getRawString());
        if (docString.isPresent()) {
            str.append(docString.get().getGherkinString().getRawString());
        } else if (dataTable.isPresent()) {
            StringBuilder tableAsString = new StringBuilder();
            // Reconstruct the cell data back into gherkin
            for (List<GherkinString> row : dataTable.get()) {
                tableAsString.append("|"); // Add leading pipe
                String rowAsString = row.stream()
                        .map(GherkinString::getRawString)
                        .collect(Collectors.joining("|"));
                tableAsString.append(rowAsString);
                tableAsString.append(String.format("|%n")); // Add ending pipe with a newline
            }
            str.append(tableAsString);
        }
        return str.toString();
    }

    public enum StepType {
        GIVEN, WHEN, THEN, AND, BUT, WILD
    }

    public static class Builder {
        private String docString = "";
        private List<List<GherkinString>> dataTable = new ArrayList<>();
        private Collection<String> comments = new ArrayList<>();
        private String stepContent;
        private String stepKeywordText;
        private StepType stepType;

        public GherkinStep build() {
            return new GherkinStep(this);
        }

        public Builder withStepContent(String stepContent) {
            this.stepContent = stepContent;
            return this;
        }

        public Builder withStepKeyword(StepType stepType, String stepKeywordText) {
            this.stepType = stepType;
            this.stepKeywordText = stepKeywordText;
            return this;
        }

        public Builder withDataTable(List<List<GherkinString>> dataTable) {
            this.dataTable = dataTable;
            return this;
        }

        public Builder withDocString(String docString) {
            this.docString = docString;
            return this;
        }

        public Builder withComments(Collection<String> comments) {
            this.comments = new ArrayList<>(comments);
            return this;
        }

        public Builder addComment(String stepComment) {
            if (stepComment != null && !stepComment.isEmpty()) {
                this.comments.add(stepComment);
            }
            return this;
        }
    }
}
