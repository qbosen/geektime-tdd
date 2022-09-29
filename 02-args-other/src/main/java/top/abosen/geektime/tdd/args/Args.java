package top.abosen.geektime.tdd.args;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author qiubaisen
 * @date 2022/9/29
 */
public class Args {
    public static Map<String, String[]> toMap(String... args) {
        Map<String, String[]> result = new HashMap<>();

        String option = null;
        List<String> values = new ArrayList<>();
        for (String arg : args) {
            if (arg.matches("^-[a-zA-Z-]+$")) {
                if (option != null) {
                    result.put(option.substring(1), values.toArray(String[]::new));
                }
                option = arg;
                values = new ArrayList<>();
            } else {
                values.add(arg);
            }
        }
        if (option != null) {
            result.put(option.substring(1), values.toArray(String[]::new));
        }

        return result;
    }
}
