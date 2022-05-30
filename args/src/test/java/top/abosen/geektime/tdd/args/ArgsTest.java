package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

    //对于输入:                          -l -p 8080 -d /usr/logs
    //可以按数组分组:                     [-l], [-p, 8080], [-d, /usr/logs]
    //也可以转化为map<str,list>:          {-l:[], -p:[8080], -d: [/usr/logs]}

    // bool -l
    @Test
    public void should_set_boolean_option_to_true_if_flag_present() {
        BooleanOption option = Args.parse(BooleanOption.class, "-l");
        assertTrue(option.logging());
    }

    @Test
    public void should_set_boolean_option_to_false_if_flag_not_present() {
        BooleanOption option = Args.parse(BooleanOption.class);
        assertFalse(option.logging());
    }


    static record BooleanOption(@Option("l") boolean logging) {
    }

    // int
    @Test
    public void should_parse_int_as_option_value() {
        IntOption option = Args.parse(IntOption.class, "-p", "8080");
        assertEquals(8080, option.port());
    }

    static record IntOption(@Option("p") int port) {
    }

    // string
    @Test
    public void should_parse_string_as_option_value() {
        StringOption option = Args.parse(StringOption.class, "-d", "/usr/logs");
        assertEquals("/usr/logs", option.directory());
    }

    static record StringOption(@Option("d") String directory) {
    }



    //example1: -l -p 8080 -d /usr/logs
    @Test
    public void should_parse_multi_options() {
        MultiOptions options = Args.parse(MultiOptions.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    static record MultiOptions(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }


    //example1: -l -p 8080 -d /usr/logs
    @Test
    @Disabled
    public void should_example_1() {
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    //example2: -g this is a list -d 1 2 -3 5
    @Test
    @Disabled
    public void should_example_2() {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new int[]{1, 2, -3, 5}, options.decimals());
    }

    static record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }

    static record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {
    }


}
