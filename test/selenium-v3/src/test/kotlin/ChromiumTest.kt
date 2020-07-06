package abs

import io.github.bonigarcia.wdm.*
import org.openqa.selenium.chrome.*

class ChromiumTest : BasedTest(){

    override fun setupDriver(){
        WebDriverManager.chromiumdriver().setup()
        val options = ChromeOptions()
        options.setHeadless(true)
        driver = ChromeDriver(options)
    }

}