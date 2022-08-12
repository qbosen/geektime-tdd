package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import top.abosen.geektime.tdd.args.exceptions.InsufficientArgumentsException;
import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static top.abosen.geektime.tdd.args.BooleanOptionParserTest.option;

/**
 * @author qiubaisen
 * @date 2022/6/15
 */
public class SingleOptionParserTest {
    @Test
    public void should_not_accept_extra_argument_for_int_single_valued_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
            new SingleValueOptionParser<>(0, Integer::parseInt).parse(asList("-p", "8080", "8081"), option("p"));
        });
        assertEquals("p", e.getOption());
    }


    @ParameterizedTest
    @ValueSource(strings = {"-p -l", "-p"})
    public void should_not_accept_insufficient_argument_for_int_single_valued_option(String arguments) {
        InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
            new SingleValueOptionParser<>(0, Integer::parseInt).parse(asList(arguments.split(" ")), option("p"));
        });
        assertEquals("p", e.getOption());
    }

    @Test
    public void should_set_default_value_to_0_for_int_option() {
        assertEquals(0, new SingleValueOptionParser<>(0, Integer::parseInt).parse(asList(), option("p")));
    }

    @Test
    public void should_parse_value_if_int_option_present() {
        assertEquals(8080, new SingleValueOptionParser<>(0, Integer::parseInt).parse(asList("-p", "8080"), option("p")));
    }

    @Test
    public void should_not_accept_extra_argument_for_string_single_valued_option() {
        TooManyArgumentsException e = assertThrows(TooManyArgumentsException.class, () -> {
            new SingleValueOptionParser<>("", String::valueOf).parse(asList("-d", "/usr/logs", "/usr/vars"), option("d"));
        });
        assertEquals("d", e.getOption());
    }


    @ParameterizedTest
    @ValueSource(strings = {"-d -l", "-d"})
    public void should_not_accept_insufficient_argument_for_string_single_valued_option(String arguments) {
        InsufficientArgumentsException e = assertThrows(InsufficientArgumentsException.class, () -> {
            new SingleValueOptionParser<>("", String::valueOf).parse(asList(arguments.split(" ")), option("d"));
        });
        assertEquals("d", e.getOption());
    }


    @Test
    public void should_set_default_value_to_empty_for_string_option() {
        assertEquals("", new SingleValueOptionParser<>("", String::valueOf).parse(asList(), option("d")));
    }

    @Test
    public void should_parse_value_if_string_option_present() {
        assertEquals("/usr/logs", new SingleValueOptionParser<>(0, String::valueOf).parse(asList("-d", "/usr/logs"), option("d")));
    }
}
