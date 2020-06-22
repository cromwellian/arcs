package arcs.core.util

import arcs.core.util.ParseResult.Failure
import arcs.core.util.ParseResult.Success
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


/** Tests for [Parser]. */
@RunWith(JUnit4::class)
class ParserTest {

    fun String.rest(pos: SourcePosition) =
        if (pos.offset >= this.length) "" else this.substring(pos.offset)

    @Test
    fun parseWithTokenizer() {
        val hello = token("hello")
        hello("hello world", 0).flatMap { match, start, end ->
            assertThat(match).isEqualTo("hello")
            assertThat("hello world".rest(end)).isEqualTo(" world")
            Success(match, start, end)
        }
        assertThat(hello("world", 0)).isInstanceOf(Failure::class.java)

        val helloregex = regex("(h.ll)o")

        helloregex("hello world", 0).flatMap { match, start, end ->
            assertThat(match).isEqualTo("hell")
            assertThat("hello world".rest(end)).isEqualTo(" world")
            Success(match, start, end)
        }
        assertThat(helloregex("world", 0)).isInstanceOf(Failure::class.java)
    }

    @Test
    fun parseSequential() {
        val helloworld = token("hello") + token("world")
        helloworld("helloworld", 0).flatMap { (hello, world), start, end ->
            assertThat(hello).isEqualTo("hello")
            assertThat(world).isEqualTo("world")
            assertThat("helloworld".rest(end)).isEqualTo("")
            Success(Pair(hello, world), start, end)
        }

        val helloworld2 = token("hello") + token("world") + token("two")
        helloworld2("helloworldtwo", 0).flatMap { (hello, world, two), start, end ->
            assertThat(hello).isEqualTo("hello")
            assertThat(world).isEqualTo("world")
            assertThat(two).isEqualTo("two")
            assertThat("helloworldtwo".rest(end)).isEqualTo("")
            Success(Triple(hello, world, two), start, end)
        }

        assertThat(helloworld("hello world", 0)).isInstanceOf(Failure::class.java)
    }

    @Test
    fun parseParallel() {
        val helloworld = token("hello") / token("goodbye") / token("foo") / token("bar")
        helloworld("hello world", 0).flatMap { match, start, end ->
            assertThat(match).isEqualTo("hello")
            assertThat("hello world".rest(end)).isEqualTo(" world")
            Success(match, start, end)
        }

        helloworld("goodbye world", 0).flatMap { match, start, end ->
            assertThat(match).isEqualTo("goodbye")
            assertThat("goodbye world".rest(end)).isEqualTo(" world")
            Success(match, start, end)
        }
        assertThat(helloworld("sayonara world", 0)).isInstanceOf(Failure::class.java)
    }

    @Test
    fun parseMany() {
        val intro = token("a ") + many(token("long ")) + token("time ago in a far away galaxy")
        val string = "a long long long time ago in a far away galaxy"
        intro(string, 0).flatMap { (_, many, _), start, end ->
            assertThat(many).containsExactly("long ", "long ", "long ")
            assertThat(string.rest(end)).isEqualTo("")
            Success(many, start, end)
        }

        assertThat(intro("a time ago", 0)).isInstanceOf(Failure::class.java)
    }

    @Test
    fun parseOptional() {
        val trailing = token("hello.") + optional(token(", "))
        trailing("hello", 0).flatMap { (hello, _), start, end ->
            assertThat(hello).isEqualTo("hello")
            assertThat("hello".rest(end)).isEqualTo(".")
            Success(hello, start, end)
        }

        trailing("hello, ", 0).flatMap { (hello, opt), start, end ->
            assertThat(hello).isEqualTo("hello")
            assertThat(opt).isEqualTo(", ")
            assertThat("hello, ".rest(end)).isEqualTo("")
            Success(hello, start, end)
        }
    }

    @Test
    fun testIgnoring() {
        val space: IgnoringParser<String> = -token(" ")
        val ignoring: SeqOf<String, String> = token("hello") + space + token("world")
        ignoring("hello world", 0).flatMap { (hello, world), start, end ->
            assertThat(hello).isEqualTo("hello")
            assertThat(world).isEqualTo("world")
            Success(hello, start, end)
        }
    }
}
