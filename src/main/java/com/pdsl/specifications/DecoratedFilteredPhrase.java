package com.pdsl.specifications;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Optional;

/**
 * A decorator for a {@link FilteredPhrase} that attaches additional metadata,
 * such as comments, to the underlying phrase.
 *
 * <p>This allows comments associated with a step/phrase in a test specification
 * to be propagated along with the parsed representation of that phrase.
 */
public class DecoratedFilteredPhrase implements FilteredPhrase {
    private final FilteredPhrase delegate;
    private final List<String> comments;

    public DecoratedFilteredPhrase(FilteredPhrase delegate, List<String> comments) {
        this.delegate = delegate;
        this.comments = comments == null ? List.of() : comments;
    }

    @Override
    public String getPhrase() {
        return delegate.getPhrase();
    }

    @Override
    public Optional<ParseTree> getParseTree() {
        return delegate.getParseTree();
    }

    @Override
    public List<String> getComments() {
        return comments;
    }
}
