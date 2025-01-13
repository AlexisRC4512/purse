package com.nttdata.yanki.purse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Log4j2
@Service
@RequiredArgsConstructor
public class CatalogCacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;


    public Mono<Object> getCatalog(String key) {
        return redisTemplate.opsForValue().get(key)
                .doOnNext(value -> log.debug("Valor obtenido de Redis para la clave {}: {}", key, value));
    }

    public Mono<Boolean> setCatalog(String key, Object value) {
        return redisTemplate.opsForValue().set(key, value)
                .doOnSuccess(success -> log.debug("Valor guardado en Redis para la clave {}: {}", key, value));
    }

    public Mono<Boolean> deleteCatalog(String key) {
        return redisTemplate.opsForValue().delete(key)
                .doOnSuccess(success -> log.debug("Clave eliminada de Redis: {}", key));
    }
}
