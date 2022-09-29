package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author qiubaisen
 * @date 2022/9/29
 */
class ArgsTest {
    // option without value -b
    @Test
    public void should_split_option_without_value() {
        Map<String, String[]> args = Args.toMap("-b");

        assertEquals(1, args.size());
        assertArrayEquals(new String[]{}, args.get("b"));
    }

    // option with value -p 8080
    @Test
    public void should_split_option_with_value() {
        Map<String, String[]> args = Args.toMap("-p", "8080");

        assertEquals(1, args.size());
        assertArrayEquals(new String[]{"8080"}, args.get("p"));
    }

    // option with values -g this is a list
    @Test
    public void should_split_option_with_values() {
        Map<String, String[]> args = Args.toMap("-g", "this", "is", "a", "list");

        assertEquals(1, args.size());
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, args.get("g"));
    }

    // multiple options
    @Test
    public void should_split_args_to_map() {
        Map<String, String[]> args = Args.toMap("-b", "-p", "8080", "-d", "/usr/logs");

        assertEquals(3, args.size());
        assertArrayEquals(new String[]{}, args.get("b"));
        assertArrayEquals(new String[]{"8080"}, args.get("p"));
        assertArrayEquals(new String[]{"/usr/logs"}, args.get("d"));
    }

    @Test
    public void should_split_args_list_to_map() {
        Map<String, String[]> args = Args.toMap("-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");

        assertEquals(2, args.size());
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, args.get("g"));
        assertArrayEquals(new String[]{"1", "2", "-3", "5"}, args.get("d"));
    }
}