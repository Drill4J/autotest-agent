import pack.HttpHeadersTest
import kotlin.test.Ignore
import kotlin.test.Test

class TestJU5 {

    @Test
    fun simpleTestMethodName() {
        HttpHeadersTest.test(::simpleTestMethodName.name)
    }

    @Test
    fun `method with backtick names`() {
        HttpHeadersTest.test(::`method with backtick names`.name)
    }

    @Suppress("RemoveRedundantBackticks")
    @Test
    @Ignore
    fun `shortBacktick`() {
        HttpHeadersTest.test(::`shortBacktick`.name)
    }
}