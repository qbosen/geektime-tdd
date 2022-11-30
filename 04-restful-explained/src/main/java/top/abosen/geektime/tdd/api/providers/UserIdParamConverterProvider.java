package top.abosen.geektime.tdd.api.providers;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import top.abosen.geektime.tdd.domain.User;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author qiubaisen
 * @date 2022/11/23
 */
@Singleton
@Provider
public class UserIdParamConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType != User.Id.class) return null;
        return new ParamConverter<T>() {
            @Override
            public T fromString(String value) {
                return (T) new User.Id(Long.parseLong(value));
            }

            @Override
            public String toString(T value) {
                return String.valueOf(((User.Id) value).value());
            }
        };
    }
}
