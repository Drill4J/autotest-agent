package abs

import io.github.bonigarcia.wdm.*
import org.junit.jupiter.api.*
import org.openqa.selenium.firefox.*

class FirefoxTest : BasedTest(){

    override fun setupDriver(){
        WebDriverManager.firefoxdriver().setup()
        val firefoxProfile = FirefoxProfile()
        val firefoxOptions = FirefoxOptions()
        firefoxOptions.setHeadless(true)
        firefoxOptions.profile = firefoxProfile
        driver = FirefoxDriver(firefoxOptions)
    }

}