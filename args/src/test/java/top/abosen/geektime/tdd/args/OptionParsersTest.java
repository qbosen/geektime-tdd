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

        // boolean: false
        @Test
        public void should_set_default_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parse(option("l"), asList()));
        }
    }

    @Nested
    class UnaryOptionParser {

        //  integer -p 8080
        @Test
        public void should_parse_integer() {
            assertEquals(8080, OptionParsers.unary(Integer::parseInt, 0).parse(option("p"), asList("-p", "8080")));
        }

        //  string -d usr/logs
        @Test
        public void should_parse_string() {
            assertEquals("usr/logs", OptionParsers.unary(String::valueOf, "").parse(option("d"), asList("-d", "usr/logs")));
        }

        //  integer: 0
        @Test
        public void should_set_default_value_to_0_if_int_option_not_present() {
            assertEquals(0, OptionParsers.unary(Integer::parseInt, 0).parse(option("p"), asList()));
        }

        //  string: ""
        @Test
        public void should_set_default_value_to_empty_string_if_string_option_not_present() {
            assertEquals("", OptionParsers.unary(String::valueOf, "").parse(option("d"), asList()));
        }

        // with wrong number args: -p 1 2 / -d usr logs
        @Test
        public void should_not_accept_too_many_arguments_for_int_option_parser() {
            assertThrows(TooManyArgumentsException.class, () ->
                    OptionParsers.unary(Integer::parseInt, 0).parse(option("p"), asList("-p", "1", "2")));
        }

        @Test
        public void should_not_accept_too_many_arguments_for_string_option_parser() {
            assertThrows(TooManyArgumentsException.class, () ->
                    OptionParsers.unary(String::valueOf, "").parse(option("d"), asList("-d", "usr", "logs")));
        }
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

        // array: []

        @Test
        public void should_set_default_value_to_empty_array_if_int_array_option_not_present() {
            assertArrayEquals(new int[]{}, OptionParsers.primaryArray(Integer::parseInt, Integer[]::new, int[].class).parse(option("d"), asList()));
        }

        @Test
        public void should_set_default_value_to_empty_array_if_string_array_option_not_present() {
            assertArrayEquals(new String[]{}, OptionParsers.array(String::valueOf, String[]::new).parse(option("g"), asList()));
        }
    }
}