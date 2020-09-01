import org.springframework.web.bind.annotation.*;

import javax.servlet.http.*;

@RestController
class Controller {

    @GetMapping("/hello")
    public String sayHello(HttpServletResponse response) {
        return "hello";
    }

    @PostMapping("/baeldung")
    public String sayHelloPost(HttpServletResponse response) {
        return "hello";
    }

    @RequestMapping(method = {RequestMethod.GET}, value = {"/version"})
    public String getVersion() {
        return "1.0";
    }

}
