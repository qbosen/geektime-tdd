package top.abosen.geektime.tdd.args;

import java.util.List;
import java.util.function.Function;

class StringValueOptionParser<T> implements OptionParser<T> {
    Function<String, T> valueParser;

    public StringValueOptionParser(Function<String, T> valueParser) {
        this.valueParser = valueParser;
    }

    @Override
    public T parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        String value = arguments.get(index + 1);
        return valueParser.apply(value);
    }
}
