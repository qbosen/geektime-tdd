package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author qiubaisen
 * @date 2022/10/8
 */
class ArgsTest {

    @Test
    public void should_parse_int_option() {
        ValueRetriever retriever = mock(ValueRetriever.class);
        OptionClass<IntOption> optionClass = mock(OptionClass.class);
        OptionParser parser = mock(OptionParser.class);

        when(optionClass.getOptionNames()).thenReturn(new String[]{"p"});
        doReturn(int.class).when(optionClass).getOptionType("p");
//        when(optionClass.getOptionType(eq("p"))).thenReturn(int.class);
        when(retriever.getValues(eq("p"), eq(new String[]{"-p", "8080"}))).thenReturn(new String[]{"8080"});
        when(parser.parse(eq(int.class), eq(new String[]{"8080"}))).thenReturn(8080);
        when(optionClass.create(eq(new Object[]{8080}))).thenReturn(new IntOption(8080));

        Args<IntOption> args = new Args<>(retriever, optionClass, parser);
        IntOption option = args.parse("-p", "8080");

        assertEquals(8080, option.port());
    }

    static record IntOption(@Option("p") int port) {

    }


    //example2: -g this is a list -d 1 2 -3 5
    @Test
    public void should_example_2() {
        Args<ListOptions> args = new Args<>(new DefaultValueRetriever(), new ReflectionBasedOptionClass<>(ListOptions.class), new OptionParsers());
        ListOptions options = args.parse("-g", "this", "is", "a", "list", "-d", "1", "2", "-3", "5");
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, options.group());
        assertArrayEquals(new Integer[]{1, 2, -3, 5}, options.decimals());
    }

    static record ListOptions(@Option("g") String[] group, @Option("d") Integer[] decimals) {
    }
}