package com.pdsl.uat.java.jupiter;

import com.pdsl.executors.InterpreterObj;
import com.pdsl.grammars.*;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.engine.descriptor.*;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class InterpreterObjStartRuleTest {

    @Test
    public void testVisitorWithCustomStartRule() {
        InterpreterObj interpreterObj = InterpreterObj.ofVisitor(
            AllGrammarsTwoParserBaseVisitor::new,
            "customStartRule"
        );

        Assertions.assertTrue(interpreterObj.getStartRule().isPresent());
        Assertions.assertEquals("customStartRule", interpreterObj.getStartRule().get());
        Assertions.assertTrue(interpreterObj.getParseTreeVisitor().isPresent());
        Assertions.assertFalse(interpreterObj.getParseTreeListener().isPresent());
    }

    @Test
    public void testVisitorWithoutStartRule() {
        InterpreterObj interpreterObj = InterpreterObj.ofVisitor(
            AllGrammarsTwoParserBaseVisitor::new
        );

        Assertions.assertFalse(interpreterObj.getStartRule().isPresent());
        Assertions.assertTrue(interpreterObj.getParseTreeVisitor().isPresent());
    }

    @Test
    public void testListenerWithCustomStartRule() {
        InterpreterObj interpreterObj = InterpreterObj.ofListener(
            AllGrammarsParserBaseListener::new,
            "customListenerRule"
        );

        Assertions.assertTrue(interpreterObj.getStartRule().isPresent());
        Assertions.assertEquals("customListenerRule", interpreterObj.getStartRule().get());
        Assertions.assertTrue(interpreterObj.getParseTreeListener().isPresent());
        Assertions.assertFalse(interpreterObj.getParseTreeVisitor().isPresent());
    }

    @Test
    public void testListenerWithoutStartRule() {
        InterpreterObj interpreterObj = InterpreterObj.ofListener(
            AllGrammarsParserBaseListener::new
        );

        Assertions.assertFalse(interpreterObj.getStartRule().isPresent());
        Assertions.assertTrue(interpreterObj.getParseTreeListener().isPresent());
    }

    @Test
    public void testListenerWithNullStartRule() {
        InterpreterObj interpreterObj = InterpreterObj.ofListener(
            AllGrammarsParserBaseListener::new,
            null
        );

        Assertions.assertFalse(interpreterObj.getStartRule().isPresent());
    }

    @Test
    public void testConstructorWithVisitorAndStartRule() {
        ParseTreeVisitor<?> visitor = new AllGrammarsTwoParserBaseVisitor<>();
        InterpreterObj interpreterObj = new InterpreterObj(visitor, "directConstructorRule");

        Assertions.assertTrue(interpreterObj.getStartRule().isPresent());
        Assertions.assertEquals("directConstructorRule", interpreterObj.getStartRule().get());
    }

    @Test
    public void testConstructorWithListenerAndStartRule() {
        ParseTreeListener listener = new AllGrammarsParserBaseListener();
        InterpreterObj interpreterObj = InterpreterObj.ofListener(()-> listener, "listenerConstructorRule");

        Assertions.assertTrue(interpreterObj.getStartRule().isPresent());
        Assertions.assertEquals("listenerConstructorRule", interpreterObj.getStartRule().get());
    }

    @Test
    public void testSupplierReturnsNewInstancesForVisitor() {
        InterpreterObj interpreterObj = InterpreterObj.ofVisitor(
            AllGrammarsTwoParserBaseVisitor::new,
            "testRule"
        );

        ParseTreeVisitor<?> visitor1 = interpreterObj.getParseTreeVisitor().get();
        ParseTreeVisitor<?> visitor2 = interpreterObj.getParseTreeVisitor().get();

        Assertions.assertNotSame(visitor1, visitor2);
        Assertions.assertEquals("testRule", interpreterObj.getStartRule().get());
    }

    @Test
    public void testSupplierReturnsNewInstancesForListener() {
        InterpreterObj interpreterObj = InterpreterObj.ofListener(
            AllGrammarsParserBaseListener::new,
            "testListenerRule"
        );

        ParseTreeListener listener1 = interpreterObj.getParseTreeListener().get();
        ParseTreeListener listener2 = interpreterObj.getParseTreeListener().get();

        Assertions.assertNotSame(listener1, listener2);
        Assertions.assertEquals("testListenerRule", interpreterObj.getStartRule().get());
    }

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
