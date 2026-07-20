package com.pdsl.specifications;

import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.util.Optional;

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
