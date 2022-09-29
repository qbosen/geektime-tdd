package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author qiubaisen
 * @date 2022/9/29
 */
class ArgsTest {

    private static String[] checkSize(String[] values, int size) {
        if (values != null && values.length != size) {
            throw new RuntimeException();
        }
        return values;
    }

    @Test
    public void should_parse_bool_option() {
        Args<BoolOption> args = new Args<>(BoolOption.class, Map.of(boolean.class, values -> checkSize(values, 0) != null));
        BoolOption option = args.parse("-l");
        assertTrue(option.logging);
    }

    @Test
    public void should_parse_int_option() {
        Args<IntOption> args = new Args<>(IntOption.class, Map.of(int.class, values -> Integer.parseInt(checkSize(values, 1)[0])));
        IntOption option = args.parse("-p", "8080");
        assertEquals(8080, option.port());
    }

    record BoolOption(@Option("l") boolean logging) {
    }

    record IntOption(@Option("p") int port) {
    }

}