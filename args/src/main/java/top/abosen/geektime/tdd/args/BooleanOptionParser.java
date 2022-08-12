package top.abosen.geektime.tdd.args;

import top.abosen.geektime.tdd.args.exceptions.TooManyArgumentsException;

import java.util.List;

class BooleanOptionParser implements OptionParser<Boolean> {
    @Override
    public Boolean parse(List<String> arguments, Option option) {
        int index = arguments.indexOf("-" + option.value());
        if (index + 1 < arguments.size() &&
                !arguments.get(index + 1).startsWith("-")) {
            throw new TooManyArgumentsException(option.value());
        }
        return index != -1;
    }
}
