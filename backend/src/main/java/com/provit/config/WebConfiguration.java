package com.provit.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.provit.common.resolver.LoginUserArgumentResolver;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration implements WebMvcConfigurer {
    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final Path uploadRoot;

    public WebConfiguration(LoginUserArgumentResolver loginUserArgumentResolver,
                            @Value("${file.upload.base-dir:D:/fileStorage_Provit}") String uploadRoot) {
        this.loginUserArgumentResolver = loginUserArgumentResolver;
        this.uploadRoot = Path.of(uploadRoot).toAbsolutePath().normalize();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**").addResourceLocations("classpath:/static/resources/");
        addPublicUploads(registry, "post_uploadfile");
        addPublicUploads(registry, "profile_uploadfile");
    }

    private void addPublicUploads(ResourceHandlerRegistry registry, String directory) {
        String location = uploadRoot.resolve(directory).toUri().toString();
        registry.addResourceHandler("/uploads/" + directory + "/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://192.168.*:*",
                        "https://*.ngrok-free.app", "https://*.ngrok-free.dev", "https://*.ngrok.io",
                        "https://*.trycloudflare.com", "https://*.vercel.app")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
