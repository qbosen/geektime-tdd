package top.abosen.geektime.tdd.rest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author qiubaisen
 * @date 2022/12/28
 */
public class ConverterTest {

    @Test
    void should_convert_via_converter_constructor() {
        assertEquals(new BigDecimal("12345"), ConverterConstructor.convert(BigDecimal.class, "12345").get());
    }

    @Test
    void should_not_convert_if_no_converter_constructor() {
        assertTrue(ConverterConstructor.convert(NoConverter.class, "12345").isEmpty());

    }

    @Test
    void should_convert_via_converter_factory() {
        assertEquals(Converter.Factory, ConverterFactory.convert(Converter.class, "Factory").get());

    }

    @Test
    void should_not_convert_if_no_converter_factory() {
        assertTrue(ConverterFactory.convert(NoConverter.class, "Factory").isEmpty());
    }
}

class NoConverter {
    NoConverter valueOf(String value){
        return new NoConverter();
    }
}

enum Converter{
    Primitive, Constructor, Factory
}