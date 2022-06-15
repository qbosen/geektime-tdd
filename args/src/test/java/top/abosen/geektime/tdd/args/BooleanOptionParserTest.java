package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class BooleanOptionParserTest {
    @Test
    public void should_not_accept_extra_argument_for_boolean_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
            new BooleanOptionParser().parse(asList("-l", "t"), option("l"));
        });

        assertEquals("l", e.getOption());
    }

    @Test
    public void should_set_default_value_to_false_if_option_not_present() {
        assertFalse((Boolean) new BooleanOptionParser().parse(asList(), option("l")));
    }

    static Option option(String value) {
        return new Option() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }
        };
    }
}
