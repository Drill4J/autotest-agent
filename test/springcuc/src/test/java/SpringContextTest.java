import org.junit.*;
import org.junit.runner.*;
import org.springframework.boot.test.context.*;
import org.springframework.test.context.junit4.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringDemoApplication.class)
public class SpringContextTest {

    @Test
    public void whenSpringContextIsBootstrapped_thenNoExceptions() {
    }
}
