package top.abosen.geektime.tdd.args;

import java.util.Arrays;

/**
 * @author qiubaisen
 * @date 2022/10/8
 */
public class Args<T> {

    private final ValueRetriever retriever;
    private final OptionClass<T> optionClass;
    private final OptionParser parser;

    public Args(ValueRetriever retriever, OptionClass<T> optionClass, OptionParser parser) {
        this.retriever = retriever;
        this.optionClass = optionClass;
        this.parser = parser;

    }

    public T parse(String... args) {
        return optionClass.create(
                Arrays.stream(optionClass.getOptionNames())
                        .map(name -> parser.parse(optionClass.getOptionType(name), retriever.getValues(name, args)))
                        .toArray(Object[]::new));
    }
}
