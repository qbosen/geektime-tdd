package top.abosen.geektime.tdd.rest;

import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.Response;

import java.lang.annotation.Annotation;

/**
 * @author qiubaisen
 * @date 2022/12/1
 */
abstract class OutboundResponse extends Response {
    abstract GenericEntity getGenericEntity();

    abstract Annotation[] getAnnotations();
}
