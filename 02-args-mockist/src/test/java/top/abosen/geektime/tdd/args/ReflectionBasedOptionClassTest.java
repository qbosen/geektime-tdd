package top.abosen.geektime.tdd.args;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author qiubaisen
 * @date 2022/10/9
 */
public class ReflectionBasedOptionClassTest {
    @Test
    public void should_treat_parameter_with_annotation_as_option() {
        OptionClass<IntOption> optionClass = new ReflectionBasedOptionClass<>(IntOption.class);

        assertArrayEquals(new String[]{"p"}, optionClass.getOptionNames());
        assertEquals(int.class, optionClass.getOptionType("p"));
        assertEquals(new IntOption(8080), optionClass.create(new Object[]{8080}));
    }

    static record IntOption(@Option("p") int port) {
    }

}
