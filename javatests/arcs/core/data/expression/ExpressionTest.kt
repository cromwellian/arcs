/*
 * Copyright 2020 Google LLC.
 *
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 *
 * Code distributed by Google as part of this project is also subject to an additional IP rights
 * grant found at
 * http://polymer.github.io/PATENTS.txt
 */

package arcs.core.data.expression

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [Expression]. */
@RunWith(JUnit4::class)
class ExpressionTest {
    fun <T> evalBool(expression: Expression<T>) = evalExpression<T, Boolean>(
        expression,
        currentScope
    )

    fun <T> evalNum(expression: Expression<T>) = evalExpression<T, Number>(
        expression = expression, currentScope = currentScope
    )

    val currentScope = CurrentScope<Any>(
        mapOf(
            "blah" to 10,
            "baz" to mapOf("x" to 24).asScope()
        )
    )

    @Test
    fun testEvaluator() {
        // numeric binary ops (ints)
        assertThat(evalNum(2.asExpr() + 1.asExpr())).isEqualTo(3)
        assertThat(evalNum(2.asExpr() - 1.asExpr())).isEqualTo(1)
        assertThat(evalNum(2.asExpr() * 2.asExpr())).isEqualTo(4)
        assertThat(evalNum(6.asExpr() / 3.asExpr())).isEqualTo(2)

        // floats
        assertThat(evalNum(2f.asExpr() + 1.asExpr())).isEqualTo(3)
        assertThat(evalNum(2f.asExpr() - 1.asExpr())).isEqualTo(1)
        assertThat(evalNum(2f.asExpr() * 2.asExpr())).isEqualTo(4)
        assertThat(evalNum(6f.asExpr() / 3.asExpr())).isEqualTo(2)

        // longs
        assertThat(evalNum(2L.asExpr() + 1.asExpr())).isEqualTo(3)
        assertThat(evalNum(2L.asExpr() - 1.asExpr())).isEqualTo(1)
        assertThat(evalNum(2L.asExpr() * 2.asExpr())).isEqualTo(4)
        assertThat(evalNum(6L.asExpr() / 3.asExpr())).isEqualTo(2)

        // big ints
        assertThat(evalNum(2.toBigInteger().asExpr() + 1.asExpr())).isEqualTo(3.toBigInteger())
        assertThat(evalNum(2.toBigInteger().asExpr() - 1.asExpr())).isEqualTo(1.toBigInteger())
        assertThat(evalNum(2.toBigInteger().asExpr() * 2.asExpr())).isEqualTo(4.toBigInteger())
        assertThat(evalNum(6.toBigInteger().asExpr() / 3.asExpr())).isEqualTo(2.toBigInteger())

        // field ops
        assertThat(evalNum<Number>(mapOf("foo" to 42).asScope()["foo"])).isEqualTo(42)
        assertThat(evalNum<Number>(currentScope["blah"].asNumber())).isEqualTo(10)
        val baz = currentScope["baz"].asScope()
        assertThat(evalNum<Number>(baz["x"])).isEqualTo(24)

        // query ops
        assertThat(evalExpression<Number, Number>(
            query("arg"),
            currentScope,
            "arg" to 42
        )).isEqualTo(42)

        // Boolean ops
        assertThat(evalBool(1.asExpr() lt 2.asExpr())).isTrue()
        assertThat(evalBool(2.asExpr() lt 1.asExpr())).isFalse()
        assertThat(evalBool(2.asExpr() lte 2.asExpr())).isTrue()
        assertThat(evalBool(3.asExpr() lte 2.asExpr())).isFalse()
        assertThat(evalBool(2.asExpr() gt 1.asExpr())).isTrue()
        assertThat(evalBool(1.asExpr() gt 2.asExpr())).isFalse()
        assertThat(evalBool(1.asExpr() gte 2.asExpr())).isFalse()
        assertThat(evalBool((1.asExpr() lt 2.asExpr()) and (2.asExpr() gt 1.asExpr()))).isTrue()
        assertThat(evalBool((2.asExpr() lt 1.asExpr()) and (2.asExpr() gt 1.asExpr()))).isFalse()
        assertThat(evalBool((1.asExpr() lt 2.asExpr()) or (2.asExpr() lt 1.asExpr()))).isTrue()
        assertThat(evalBool((1.asExpr() gt 2.asExpr()) or (2.asExpr() lt 1.asExpr()))).isFalse()

        // Sanity check longs and widening
        assertThat(evalBool(1L.asExpr() lt 2.asExpr())).isTrue()
        assertThat(evalBool(2L.asExpr() lt 1.asExpr())).isFalse()
        assertThat(evalBool(2L.asExpr() lte 2.asExpr())).isTrue()
        assertThat(evalBool(3L.asExpr() lte 2.asExpr())).isFalse()
        assertThat(evalBool(2L.asExpr() gt 1.asExpr())).isTrue()
        assertThat(evalBool(1L.asExpr() gt 2.asExpr())).isFalse()
        assertThat(evalBool(1L.asExpr() gte 2.asExpr())).isFalse()
        assertThat(evalBool((1L.asExpr() lt 2.asExpr()) and (2L.asExpr() gt 1.asExpr()))).isTrue()
        assertThat(evalBool((2L.asExpr() lt 1.asExpr()) and (2L.asExpr() gt 1.asExpr()))).isFalse()
        assertThat(evalBool((1L.asExpr() lt 2.asExpr()) or (2L.asExpr() lt 1.asExpr()))).isTrue()
        assertThat(evalBool((1L.asExpr() gt 2.asExpr()) or (2L.asExpr() lt 1.asExpr()))).isFalse()

        // Sanity check BigInteger
        assertThat(evalBool(1.toBigInteger().asExpr() lt 2.asExpr())).isTrue()
        assertThat(evalBool(2.toBigInteger().asExpr() lt 1.asExpr())).isFalse()
        assertThat(evalBool(2.toBigInteger().asExpr() lte 2.asExpr())).isTrue()
        assertThat(evalBool(3.toBigInteger().asExpr() lte 2.asExpr())).isFalse()
        assertThat(evalBool(2.toBigInteger().asExpr() gt 1.asExpr())).isTrue()
        assertThat(evalBool(1.toBigInteger().asExpr() gt 2.asExpr())).isFalse()
        assertThat(evalBool(1.toBigInteger().asExpr() gte 2.asExpr())).isFalse()
        assertThat(evalBool((1.toBigInteger().asExpr() lt 2.asExpr()) and
            (2.toBigInteger().asExpr() gt 1.toBigInteger().asExpr()))).isTrue()
        assertThat(evalBool((2.toBigInteger().asExpr() lt 1.asExpr()) and
            (2.toBigInteger().asExpr() gt 1.toBigInteger().asExpr()))).isFalse()
        assertThat(evalBool((1.toBigInteger().asExpr() lt 2.asExpr()) or
            (2.toBigInteger().asExpr() lt 1.toBigInteger().asExpr()))).isTrue()
        assertThat(evalBool((1.toBigInteger().asExpr() gt 2.asExpr()) or
            (2.toBigInteger().asExpr() lt 1.toBigInteger().asExpr()))).isFalse()
        // Unary ops
        assertThat(evalNum(-2.asExpr())).isEqualTo(-2)
        assertThat(evalBool(!(2.asExpr() lt 1.asExpr()))).isTrue()
        assertThat(evalBool(!(2.asExpr() gt 1.asExpr()))).isFalse()

        assertThat(evalNum(-2L.asExpr())).isEqualTo(-2)
        assertThat(evalBool(!(2L.asExpr() lt 1.asExpr()))).isTrue()
        assertThat(evalBool(!(2L.asExpr() gt 1.asExpr()))).isFalse()

        assertThat(evalNum(-2.toBigInteger().asExpr())).isEqualTo(-2.toBigInteger())
        assertThat(evalBool(!(2.toBigInteger().asExpr() lt 1.asExpr()))).isTrue()
        assertThat(evalBool(!(2.toBigInteger().asExpr() gt 1.asExpr()))).isFalse()

        // Equality ops
        assertThat(evalBool(2.asExpr() eq 2.asExpr())).isTrue()
        assertThat(evalBool(2.asExpr() eq 1.asExpr())).isFalse()
        assertThat(evalBool("Hello".asExpr() eq "Hello".asExpr())).isTrue()
        assertThat(evalBool("Hello".asExpr() eq "World".asExpr())).isFalse()
        assertThat(evalBool(true.asExpr() eq true.asExpr())).isTrue()
        assertThat(evalBool(true.asExpr() eq false.asExpr())).isFalse()
        assertThat(evalBool(2.asExpr() neq 1.asExpr())).isTrue()
        assertThat(evalBool(2.asExpr() neq 2.asExpr())).isFalse()
        assertThat(evalBool("Hello".asExpr() neq "World".asExpr())).isTrue()
        assertThat(evalBool("Hello".asExpr() neq "Hello".asExpr())).isFalse()
        assertThat(evalBool(true.asExpr() neq false.asExpr())).isTrue()
        assertThat(evalBool(true.asExpr() neq true.asExpr())).isFalse()

        // Test complex expression
        // (2 + (3 * 4) + scope.foo + ?arg - 1) / 2
        val obj = mapOf("foo" to 42).asScope("handle")
        val expr = (2.0.asExpr() + (3.asExpr() * 4.asExpr()) + obj["foo"] + query(
            "arg"
        ) - 1.asExpr()) / 2.asExpr()

        assertThat(evalExpression<Number, Number>(expr, currentScope, "arg" to 1)).isEqualTo(28)
    }

    @Test
    fun testExpressionWithScopeLookupError() {
        val obj = mapOf("foo" to 42).asScope("handle")
        val expr = 1.asExpr() + obj["bar"]
        assertFailsWith<IllegalArgumentException> {
            evalNum(expr)
        }
    }

    @Test
    fun testExpressionWithQueryParamLookupError() {
        val expr = 1.asExpr() + query("arg")
        assertFailsWith<IllegalArgumentException> {
            evalNum(expr)
        }
    }

    @Test
    fun testStringify() {
        // Test Math binary ops, field lookups, and parameter lookups
        // (2 + (3 * 4) + scope.foo + ?arg - 1) / 2
        val obj = mapOf("foo" to 42).asScope("handle")
        val expr = (2.0.asExpr() + (3.asExpr() * 4.asExpr()) + obj["foo"] + query(
            "arg"
        ) - 1.asExpr()) / 2.asExpr()
        assertThat(expr.toString()).isEqualTo("((((2.0 + (3 * 4)) + handle.foo) + ?arg) - 1) / 2")
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testJsonSerialization() {
        val q = query<Expression.Scope>("arg")
        val field = Expression.FieldExpression<Expression.Scope, Number>(q, "bar")
        val baz = currentScope.get(
            "baz"
        ) as Expression.FieldExpression<CurrentScope<Any>, Expression.Scope>
        val x: Expression<Number> = baz["x"]
        val expr = (x + 2.0.asExpr() + (3f.asExpr() * 4L.asExpr()) + field - 1.toByte().asExpr()) /
            2.toBigInteger().asExpr()
        val json = expr.serialize()
        val parsed = json.deserializeExpression() as Expression<Number>
        assertThat(evalExpression<Number, Number>(
            parsed,
            currentScope,
            "arg" to mapOf("bar" to 5).asScope()
        )).isEqualTo(21.0)
    }
}
