package top.abosen.geektime.tdd.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author qiubaisen
 * @date 2022/12/28
 */
public class ConverterConstructorTest {

    @Test
    void should_convert_via_converter_constructor() {
        assertEquals(new BigDecimal("12345"), ConverterConstructor.convert(BigDecimal.class, "12345").get());
    }

}
