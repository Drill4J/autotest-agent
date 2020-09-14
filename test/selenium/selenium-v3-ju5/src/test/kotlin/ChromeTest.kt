package abs

import io.github.bonigarcia.wdm.*
import org.junit.jupiter.api.*
import org.openqa.selenium.chrome.*

class ChromeTest : BasedTest(){

    override fun setupDriver(){
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions()
        options.setHeadless(true)
        driver = ChromeDriver(options)
    }

    @AfterEach
    fun checkNonAppPage() {
        driver.get("chrome://version/")
        driver.manage().deleteAllCookies()
    }

}