package top.abosen.geektime.tdd.args;

import java.util.List;
import java.util.stream.IntStream;

/**
 * @author qiubaisen
 * @date 2022/10/9
 */
public class DefaultValueRetriever implements ValueRetriever {
    @Override
    public String[] getValues(String name, String[] args) {
        List<String> argsList = List.of(args);
        int startIndex = argsList.indexOf("-" + name);
        if (startIndex < 0) {
            return null;
        }

        int nextOptionIndex = IntStream.range(startIndex + 1, argsList.size())
                .filter(it -> argsList.get(it).matches("^-[a-zA-Z-]+$"))
                .findFirst().orElseGet(argsList::size);

        return argsList.subList(startIndex + 1, nextOptionIndex).toArray(String[]::new);
    }
}
