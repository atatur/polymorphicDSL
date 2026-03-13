package com.pdsl.uat.java.jupiter;

import com.pdsl.executors.InterpreterObj;
import com.pdsl.grammars.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.descriptor.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class InterpreterObjStartRuleTest {

    @TestTemplate
    @ExtendWith(PdslExtension.class)
    public void pdslGherkinTestFrameworkResources(PdslExecutable executable) {
        executable.execute();
    }

    private static class PdslExtension extends PdslSharedInvocationContextProvider {

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
            return getInvocationContext(PdslConfigParameter.createGherkinPdslConfig(
                            List.of(
                                    new PdslTestParameter.Builder(
                                            List.of(
                                                    new Interpreter(AllGrammarsLexer.class, AllGrammarsParser.class,
                                                            InterpreterObj.ofListener(AllGrammarsParserBaseListener::new, "polymorphicDslAllRules")),
                                                    new Interpreter(AllGrammarsLexer.class, AllGrammarsTwoParser.class,
                                                            InterpreterObj.ofVisitor(AllGrammarsTwoParserBaseVisitor::new))
                                            )
                                    )
                                            .withStartRule("customStartRule")
                                            .build()
                            )
                    )
                    .withDslRecognizerLexer(AllGrammarsLexer.class)
                    .withDslRecognizerParser(AllGrammarsParser.class)
                    .withApplicationName("Polymorphic DSL Framework")
                    .withContext("User Acceptance Test")
                    .withResourceRoot(Paths.get("src/test/resources/framework_specifications/features/interpreter/").toUri())
                    .withRecognizerRule("polymorphicDslAllRules")
                    .build())
                    .stream();
        }
    }
}
