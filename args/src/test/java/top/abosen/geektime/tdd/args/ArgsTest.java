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
    // todo boolean -l
    // todo integer -p 8080
    // todo string -d usr/logs
    // todo array: -g this is/ -d 1 2 -3


    // todo -l -p 8080 -d /usr/logs
    @Test
    @Disabled
    public void should_example_1() {
        record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
        }
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging);
        assertEquals(8080, options.port);
        assertEquals("/usr/logs", options.directory);
    }


    // todo -g this is a list -d 1 2 -3 5
    @Test
    @Disabled
    public void should_example_2() {
        record Options(@Option("g") String[] groups, @Option("d") int[] decimals) {
        }
        Options options = Args.parse(Options.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.groups);
        assertEquals(new int[]{1, 2, -3, 5}, options.decimals);
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
