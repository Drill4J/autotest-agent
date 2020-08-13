import org.junit.experimental.categories.Category
import spock.lang.Specification
 
@Category(UnitTest.class)
class MessageServiceSpec extends Specification {
 
    def 'Get message'() {
        URLConnection con = (URLConnection)new URL("http://postman-echo.com/headers").openConnection()
        con.requestMethod = "GET"
        def bytes = con.inputStream.readAllBytes()
        expect: 'Should return the correct message'
        println 'Should return the correct message'
        new String(bytes).contains("Get message")
    }

    def "numbers to the power of two"(int a, int b, int c) {
        URLConnection con = (URLConnection)new URL("http://postman-echo.com/headers").openConnection()
        con.requestMethod = "GET"
        def bytes = con.inputStream.readAllBytes()
        expect:
        new String(bytes).contains("numbers to the power of two")

        where:
        a | b | c
        1 | 2 | 1
        2 | 2 | 4
        3 | 2 | 9
    }
}