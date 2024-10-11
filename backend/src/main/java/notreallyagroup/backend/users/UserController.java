package notreallyagroup.backend.users;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class Controller {
    @RequestMapping("login")
    protected String login(@RequestParam("username") String username, @RequestParam("password") String password) {
        return username.equals(password) ? "equal" : "not equal";
    }
}
