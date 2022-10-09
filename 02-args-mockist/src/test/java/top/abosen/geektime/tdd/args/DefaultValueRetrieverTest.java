package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author qiubaisen
 * @date 2022/10/9
 */
class DefaultValueRetrieverTest {
    @Test
    public void should_be_null_if_option_not_present() {
        ValueRetriever retriever = new DefaultValueRetriever();

        String[] values = retriever.getValues("p", new String[]{"-b", "-d", "1", "-2"});
        assertNull( values);
    }

    @Test
    public void should_find_single_values() {
        ValueRetriever retriever = new DefaultValueRetriever();

        String[] values = retriever.getValues("p", new String[]{"-p", "8080"});
        assertArrayEquals(new String[]{"8080"}, values);
    }

    @Test
    public void should_find_multiple_values() {
        ValueRetriever retriever = new DefaultValueRetriever();

        String[] values = retriever.getValues("d", new String[]{"-p", "8080", "-b", "-d", "1", "2", "-3"});
        assertArrayEquals(new String[]{"1", "2", "-3"}, values);
    }

}