package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.abosen.geektime.tdd.args.exceptions.IllegalOptionException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {

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

    @Test
    public void should_throw_illegal_option_exception_if_annotation_not_present() {
        IllegalOptionException e = assertThrows(IllegalOptionException.class, () -> {
            Args.parse(OptionsWithoutAnnotation.class, "-l", "-p", "8080", "-d", "/usr/logs");
        });

        assertEquals("port", e.getParameter());
    }

    static record OptionsWithoutAnnotation(@Option("l") boolean logging, int port, @Option("d") String directory) {
    }

    //example1: -l -p 8080 -d /usr/logs
    @Test
    public void should_example_1() {
        Options options = Args.parse(Options.class, "-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    //example2: -g this is a list -d 1 2 -3 5
    @Test
    public void should_example_2() {
        ListOptions options = Args.parse(ListOptions.class, "-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new Integer[]{1, 2, -3, 5}, options.decimals());
    }

    static record Options(@Option("l") boolean logging, @Option("p") int port, @Option("d") String directory) {
    }

    static record ListOptions(@Option("g") String[] group, @Option("d") Integer[] decimals) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void should_parse_options_if_option_parser_provided() {
        OptionParser boolParser = Mockito.mock(OptionParser.class);
        OptionParser intParser = Mockito.mock(OptionParser.class);
        OptionParser stringParser = Mockito.mock(OptionParser.class);

        Mockito.when(boolParser.parse(Mockito.anyList(), Mockito.any())).thenReturn(true);
        Mockito.when(intParser.parse(Mockito.anyList(), Mockito.any())).thenReturn(1000);
        Mockito.when(stringParser.parse(Mockito.anyList(), Mockito.any())).thenReturn("parsed");

        Args<MultiOptions> args = new Args<>(MultiOptions.class, Map.of(boolean.class, boolParser, int.class, intParser, String.class, stringParser));

        MultiOptions options = args.parse("-l", "-p", "8080", "-d", "/usr/logs");
        assertTrue(options.logging());
        assertEquals(1000, options.port());
        assertEquals("parsed", options.directory());
    }
}
