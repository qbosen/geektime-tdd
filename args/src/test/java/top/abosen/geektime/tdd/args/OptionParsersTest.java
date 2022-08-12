package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.abosen.geektime.tdd.args.exceptions.InsufficientArgumentsException;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.lang.annotation.Annotation;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static top.abosen.geektime.tdd.args.OptionParsersTest.BooleanOptionParserTest.option;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class OptionParsersTest {

    @Nested
    class UnaryOptionParser {

        @Test
        public void should_not_accept_extra_argument_for_int_single_valued_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parse(asList("-p", "8080", "8081"), option("p"));
            });
            assertEquals("p", e.getOption());
        }


        @ParameterizedTest
        @ValueSource(strings = {"-p -l", "-p"})
        public void should_not_accept_insufficient_argument_for_int_single_valued_option(String arguments) {
            InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
                OptionParsers.unary(0, Integer::parseInt).parse(asList(arguments.split(" ")), option("p"));
            });
            assertEquals("p", e.getOption());
        }

        @Test
        public void should_set_default_value_to_0_for_int_option() {
            assertEquals(0, OptionParsers.unary(0, Integer::parseInt).parse(asList(), option("p")));
        }

        @Test
        public void should_parse_value_if_int_option_present() {
            assertEquals(8080, OptionParsers.unary(0, Integer::parseInt).parse(asList("-p", "8080"), option("p")));
        }

        @Test
        public void should_not_accept_extra_argument_for_string_single_valued_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.unary("", String::valueOf).parse(asList("-d", "/usr/logs", "/usr/vars"), option("d"));
            });
            assertEquals("d", e.getOption());
        }


        @ParameterizedTest
        @ValueSource(strings = {"-d -l", "-d"})
        public void should_not_accept_insufficient_argument_for_string_single_valued_option(String arguments) {
            InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
                OptionParsers.unary("", String::valueOf).parse(asList(arguments.split(" ")), option("d"));
            });
            assertEquals("d", e.getOption());
        }


        @Test
        public void should_set_default_value_to_empty_for_string_option() {
            assertEquals("", OptionParsers.unary("", String::valueOf).parse(asList(), option("d")));
        }

        @Test
        public void should_parse_value_if_string_option_present() {
            assertEquals("/usr/logs", OptionParsers.unary(0, String::valueOf).parse(asList("-d", "/usr/logs"), option("d")));
        }
    }

    @Nested
    class BooleanOptionParserTest {
        @Test//Sad path
        public void should_not_accept_extra_argument_for_boolean_option() {
            TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
                OptionParsers.bool().parse(asList("-l", "t"), option("l"));
            });

            assertEquals("l", e.getOption());
        }

        @Test//Default value
        public void should_set_default_value_to_false_if_option_not_present() {
            assertFalse(OptionParsers.bool().parse(asList(), option("l")));
        }

        @Test//Happy Path
        public void should_set_value_to_true_if_option_present() {
            assertTrue(OptionParsers.bool().parse(asList("-l"), option("l")));
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
}
