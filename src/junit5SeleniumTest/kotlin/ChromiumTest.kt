package abs

import io.github.bonigarcia.wdm.*
import org.junit.jupiter.api.*
import org.openqa.selenium.chrome.*
import org.openqa.selenium.firefox.*

class ChromiumTest : BasedTest(){

    override fun setupDriver(){
        WebDriverManager.chromiumdriver().setup()
        val options = ChromeOptions()
        options.setHeadless(true)
        driver = ChromeDriver(options)
    }

}