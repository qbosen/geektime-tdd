package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;
import top.abosen.geektime.tdd.args.exceptions.IllegalOptionException;
import top.abosen.geektime.tdd.args.exceptions.IllegalValueException;
import top.abosen.geektime.tdd.args.exceptions.UnsupportedOptionTypeException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author qiubaisen
 * @date 2022/8/29
 */
public class ArgsTest {


    // with no option annotation present
    @Test
    public void should_throw_exception_if_option_annotation_not_present() {
        record Options(boolean logging, @Option("p") int port) {
        }
        IllegalOptionException e = assertThrows(IllegalOptionException.class, () -> Args.parse(Options.class, "-l", "-p", "8080"));
        assertEquals("logging", e.getParameter());
    }

    @Test
    public void should_throw_exception_if_option_type_not_support() {
        record Options(@Option("p") Object port) {
        }
        UnsupportedOptionTypeException e = assertThrows(UnsupportedOptionTypeException.class, () -> Args.parse(Options.class, "-l", "-p", "8080"));
        assertEquals("Object", e.getType());
    }

    // with incompatible arg: -p hello
    @Test
    public void should_throw_exception_if_argument_type_incompatible() {
        record Options(@Option("p") int port) {
        }
        IllegalValueException e = assertThrows(IllegalValueException.class, () -> Args.parse(Options.class, "-p", "hello"));
        assertEquals("p", e.getOption());
        assertEquals("hello", e.getValue());
    }

    // example1: -l -p 8080 -d /usr/logs
    @Test
    public void should_example_1() {
        record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
        }
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }


    // -g this is a list -d 1 2 -3 5
    @Test
    public void should_example_2() {
        record Options(@Option("g") String[] groups, @Option("d") int[] decimals) {
        }
        Options options = Args.parse(Options.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.groups());
        assertArrayEquals(new int[]{1, 2, -3, 5}, options.decimals());
    }


}
