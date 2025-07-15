package es.musicalia.gestmusica.config;

import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cacheManager -> {
            // Aquí puedes personalizar caches desde código si quieres
            // pero con ehcache.xml no es necesario añadir nada aquí
        };
    }

    @Bean
    public CacheManager cacheManager() {
        return new JCacheCacheManager(createEhCacheManager());
    }

    private javax.cache.CacheManager createEhCacheManager() {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        URI uri = null;
        try {
            uri = getClass().getResource("/ehcache.xml").toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return cachingProvider.getCacheManager(uri, getClass().getClassLoader());
    }
}
