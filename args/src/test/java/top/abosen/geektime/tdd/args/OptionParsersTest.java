package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.lang.annotation.Annotation;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/9/3
 */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
class OptionParsersTest {

    static Option option(String flag) {
        return new Option() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Option.class;
            }

            @Override
            public String value() {
                return flag;
            }
        };
    }

    @Nested
    class BooleanOptionParser {
        record Options(@Option("l") boolean logging) {
        }

        // boolean -l
        @Test
        public void should_parse_bool() {
            assertTrue(OptionParsers.bool().parse(option("l"), asList("-l")));
        }

        // boolean with args: -l 1/ -l 1 2
        @ParameterizedTest
        @ValueSource(strings = {"-l 1", "-l 1 2"})
        public void should_not_accept_extra_argument_for_boolean_option(String arguments) {
            assertThrows(TooManyArgumentsException.class, () ->
                    OptionParsers.bool().parse(option("l"), asList(arguments.split("\\s+")))
            );
        }
        // todo boolean: false

    }

    @Nested
    class UnaryOptionParser {

        //  integer -p 8080
        @Test
        public void should_parse_integer() {
            assertEquals(8080, OptionParsers.unary(Integer::parseInt).parse(option("p"), asList("-p","8080")));
        }

        //  string -d usr/logs
        @Test
        public void should_parse_string() {
            assertEquals("usr/logs", OptionParsers.unary(String::valueOf).parse(option("d"), asList("-d", "usr/logs")));
        }
        // todo integer: 0
        // todo string: ""
    }

    @Nested
    class ArrayOptionParser {


        // array: -g this is/ -d 1 2 -3
        @Test
        public void should_parse_string_array() {
            assertArrayEquals(new String[]{"this", "is"}, OptionParsers.array(String::valueOf, String[]::new).parse(option("g"), asList("-g", "this", "is")));
        }

        @Test
        public void should_parse_integer_array() {
            assertArrayEquals(new int[]{1, 2, -3}, OptionParsers.primaryArray(Integer::parseInt, Integer[]::new, int[].class).parse(option("d"), asList("-d", "1", "2", "-3")));
        }
        // todo array: []

    }
}