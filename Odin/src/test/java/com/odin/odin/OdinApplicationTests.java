package com.odin.odin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

@SpringBootTest
class OdinApplicationTests {

    static {
        try {
            Method m = OdinApplication.class.getDeclaredMethod("loadDotEnv");
            m.setAccessible(true);
            m.invoke(null);
        } catch (Exception ignored) {}
    }

    @Test
    void contextLoads() {
    }

}
