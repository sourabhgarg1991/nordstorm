package data.provider;

import org.testng.annotations.DataProvider;

public class LoginCredDataProviders {

    @DataProvider(name = "loginData")
    public Object[][] getLoginData() {
        return new Object[][] {
                {"user1", "pass1"},
                {"user2", "pass2"},
                {"user3", "pass3"}
        };
    }
}