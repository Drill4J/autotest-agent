package abs

import io.github.bonigarcia.wdm.*
import org.openqa.selenium.chrome.*

class ChromeTest : BasedTest(){

    override fun setupDriver(){
        WebDriverManager.chromedriver().setup()
        val options = ChromeOptions()
        options.setHeadless(true)
        driver = ChromeDriver(options)
    }

}