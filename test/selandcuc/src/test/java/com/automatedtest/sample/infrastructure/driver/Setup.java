package com.automatedtest.sample.infrastructure.driver;

import com.sun.net.httpserver.*;
import io.cucumber.java.*;
import io.github.bonigarcia.wdm.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.firefox.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class Setup {

    public static WebDriver driver;
    public static HttpServer httpServer;
    public static Set<String> testNames = new LinkedHashSet<>();
    public static int port;

    @Before
    public void setWebDriver() throws Exception {

        String browser = System.getProperty("browser");
        if (browser == null) {
            browser = "chrome";
        }
        switch (browser) {
            case "chrome":
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("['start-maximized']");
                chromeOptions.setHeadless(true);
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(chromeOptions);
                break;
            case "firefox":
                driver = new FirefoxDriver();
                driver.manage().window().maximize();
                break;
            default:
                throw new IllegalArgumentException("Browser \"" + browser + "\" isn't supported.");
        }
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/1", httpExchange -> {
                    String status = "OK";
                    testNames.add(httpExchange.getRequestHeaders().getFirst("drill-test-name"));
                    httpExchange.sendResponseHeaders(200, status.getBytes().length);
                    OutputStream os = httpExchange.getResponseBody();
                    os.write(status.getBytes());
                    httpExchange.close();
                }
        );
        httpServer.setExecutor(null);
        port = httpServer.getAddress().getPort();
        httpServer.start();
    }

    @After
    public void stopHttpServer() {
        httpServer.stop(0);
    }
}
