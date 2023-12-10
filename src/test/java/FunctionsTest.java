import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FunctionsTest {

    @Test
    void test() {
        assertEquals(Set.of("INT", "SIN"), Functions.getFunctionNames());
    }

}