package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class ArgsTest {
    // happy path
    // boolean -l
    @Test
    public void should_parse_bool() {
        record Options(@Option("l") boolean logging) {
        }
        Options options = Args.parse(Options.class, "-l");
        assertTrue(options.logging());
    }

    //  integer -p 8080
    @Test
    public void should_parse_integer() {
        record Options(@Option("p") int port) {
        }
        Options options = Args.parse(Options.class, "-p", "8080");
        assertEquals(8080, options.port());
    }

    //  string -d usr/logs
    @Test
    public void should_parse_string() {
        record Options(@Option("d") String directory) {
        }
        Options options = Args.parse(Options.class, "-d", "usr/logs");
        assertEquals("usr/logs", options.directory());
    }

    // todo array: -g this is/ -d 1 2 -3
    @Test
    public void should_parse_string_array() {
        record Options(@Option("g") String[] groups) {
        }
        Options options = Args.parse(Options.class, "-g", "this", "is");
        assertArrayEquals(new String[]{"this", "is"}, options.groups());
    }

    @Test
    public void should_parse_integer_array() {
        record Options(@Option("d") int[] decimals) {
        }
        Options options = Args.parse(Options.class, "-d", "1", "2", "-3");
        assertArrayEquals(new int[]{1, 2, -3}, options.decimals());
    }


    // todo example1: -l -p 8080 -d /usr/logs
    @Test
    @Disabled
    public void should_example_1() {
        record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
        }
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }


    // todo -g this is a list -d 1 2 -3 5
    @Test
    @Disabled
    public void should_example_2() {
        record Options(@Option("g") String[] groups, @Option("d") int[] decimals) {
        }
        Options options = Args.parse(Options.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.groups());
        assertEquals(new int[]{1, 2, -3, 5}, options.decimals());
    }

    // sad path
    // todo boolean with args: -l 1/ -l 1 2
    // todo with wrong number args: -p 1 2 / -d usr logs
    // todo with incompatible arg: -p hello

    // default path
    // todo boolean: false
    // todo integer: 0
    // todo string: ""
    // todo array: []
}
