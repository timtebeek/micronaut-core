package io.micronaut.expressions

import io.micronaut.annotation.processing.test.AbstractEvaluatedExpressionsSpec;


class MethodArgumentEvaluationContextExpressionsSpec extends AbstractEvaluatedExpressionsSpec {

    void "test method argument access"() {
        given:
        Object expr1 = buildSingleExpressionFromClass("test.Expr", """
            package test;
            import io.micronaut.context.annotation.Executable;
            import io.micronaut.context.annotation.Requires;
            import jakarta.inject.Singleton;

            @Singleton
            class Expr {

                @Executable
                @Requires(value = "#{ #second + 'abc' }")
                void test(String first, String second) {
                }
            }


        """)
        .evaluate("arg0", "arg1")

        expect:
        expr1 instanceof String && expr1 == 'arg1abc'
    }

}