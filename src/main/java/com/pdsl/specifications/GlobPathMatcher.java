package com.pdsl.specifications;

import com.google.common.base.Preconditions;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Optional;

public class GlobPathMatcher implements PathMatcher {

    private static final String GLOB_PREFIX = "glob:";
    private final List<PathMatcher> includes;
    private final Optional<List<PathMatcher>> excludes;

    private static String normalizeExpression(String expression) {
        String normalized = expression;
        return String.format("%s%s", GLOB_PREFIX, normalized);
    }

    private static PathMatcher getMatcher(String expression) {
        return FileSystems.getDefault()
                .getPathMatcher(expression.startsWith(GLOB_PREFIX) ? expression : GLOB_PREFIX + expression);
    }

    private static List<PathMatcher> getMatcher(List<String> expressions) {
        return expressions.stream()
                .map(GlobPathMatcher::getMatcher)
                .toList();
    }

    public GlobPathMatcher(String includes, String excludes) {
        Preconditions.checkNotNull(includes);
        Preconditions.checkNotNull(excludes);
        this.includes = List.of(getMatcher(includes));
        this.excludes = Optional.of(List.of(getMatcher(excludes)));
    }

    public GlobPathMatcher(String includes) {
        Preconditions.checkNotNull(includes);
        this.includes = List.of(getMatcher(includes));
        this.excludes = Optional.empty();
    }

    public GlobPathMatcher(List<String> includeExpressions) {
        Preconditions.checkNotNull(includeExpressions);
        this.includes = getMatcher(includeExpressions);
        this.excludes = Optional.empty();
    }

    public GlobPathMatcher(List<String> includeExpressions, List<String> excludeExpressions) {
        Preconditions.checkNotNull(includeExpressions);
        Preconditions.checkNotNull(excludeExpressions);
        this.includes = getMatcher(includeExpressions);
        this.excludes = Optional.of(getMatcher(excludeExpressions));
    }

    public GlobPathMatcher(String includeExpression, List<String> excludeExpressions) {
        Preconditions.checkNotNull(includeExpression);
        Preconditions.checkNotNull(excludeExpressions);
        this.includes = List.of(getMatcher(includeExpression));
        this.excludes = Optional.of(getMatcher(excludeExpressions));
    }

    public GlobPathMatcher(List<String> includeExpressions, String excludeExpression) {
        Preconditions.checkNotNull(includeExpressions);
        Preconditions.checkNotNull(excludeExpression);
        this.includes = getMatcher(includeExpressions);
        this.excludes = Optional.of(List.of(getMatcher(excludeExpression)));
    }

    @Override
    public boolean matches(Path path) {
        Path normalizedPath = (path.isAbsolute() ? path : path.toAbsolutePath()).normalize();
        return excludes.map(pathMatchers -> includes.stream().anyMatch(matcher -> matcher.matches(normalizedPath))
                && pathMatchers.stream().noneMatch(matcher -> matcher.matches(normalizedPath)))
                .orElseGet(() -> includes.stream().anyMatch(matcher -> matcher.matches(normalizedPath)));
    }

}
