import pack.HttpHeadersTest
import kotlin.test.Test

class Test {

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
    fun `shortBacktick`() {
        HttpHeadersTest.test(::`shortBacktick`.name)
    }
}