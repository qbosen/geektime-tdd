package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mockito;
import top.abosen.geektime.tdd.args.exceptions.IllegalValueException;
import top.abosen.geektime.tdd.args.exceptions.InsufficientArgumentsException;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static top.abosen.geektime.tdd.args.OptionParsersTest.BooleanOptionParserTest.option;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class OptionParsersTest {

    @Nested
    class UnaryOptionParser {

        @Test
        public void should_parse_value_if_option_present__behavior_verification() {
            Function<String, Object> parser = mock(Function.class);

            OptionParsers.unary(Mockito.any(), parser).parse(asList("-p", "8080"), option("p"));
            // 确认接收到了一次指定参数的调用
            Mockito.verify(parser).apply("8080");
        }

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

    @Nested
    class ListOptionParserTest {
        @Test
        public void should_parse_list_value() {
            Function<String, Object> parser = mock(Function.class);

            OptionParsers.list(Object[]::new, parser).parse(asList("-g", "this", "is"), option("g"));

            // 带顺序的验证
            InOrder order = Mockito.inOrder(parser, parser);
            order.verify(parser).apply("this");
            order.verify(parser).apply("is");
        }

        @Test
        public void should_not_treat_negative_int_as_flag() {
            Integer[] value = OptionParsers.list(Integer[]::new, Integer::parseInt).parse(asList("-d", "-1", "-2"), option("d"));
            assertArrayEquals(new Integer[]{-1, -2}, value);
        }


        @Test
        public void should_use_empty_array_as_default_value() {
            String[] value = OptionParsers.list(String[]::new, String::valueOf).parse(asList(), option("g"));
            assertEquals(0, value.length);
        }

        @Test
        public void should_throw_exception_if_value_parser_cant_parse_value() {
            IllegalValueException e = assertThrows(IllegalValueException.class,
                    () -> OptionParsers.list(String[]::new, Integer::parseInt).parse(asList("-d", "a"), option("d")));
            assertEquals("d", e.getOption());
            assertEquals("a", e.getValue());
        }
    }
}
