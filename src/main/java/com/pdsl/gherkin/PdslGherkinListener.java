package com.pdsl.gherkin;

import com.pdsl.gherkin.models.GherkinFeature;
import com.pdsl.gherkin.parser.GherkinParserBaseListener;
import org.antlr.v4.runtime.CommonTokenStream;

import java.net.URI;
import java.util.Optional;

public abstract class PdslGherkinListener extends GherkinParserBaseListener {

    public abstract Optional<GherkinFeature> getGherkinFeature(URI featurePathOrId);

    public abstract void setTokenStream(CommonTokenStream tokens);
}
