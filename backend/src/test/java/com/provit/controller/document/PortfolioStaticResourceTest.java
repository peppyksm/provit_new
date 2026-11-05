package com.provit.controller.document;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.provit.common.resolver.LoginUserArgumentResolver;
import com.provit.config.WebConfiguration;

public class PortfolioStaticResourceTest {
    @Rule public TemporaryFolder directory = new TemporaryFolder();

    @Test
    public void realResourceMappingsHideExistingPortfoliosAndPreservePostAndProfileImages() throws Exception {
        Path publicRoot = directory.newFolder("public").toPath();
        Files.write(Files.createDirectories(publicRoot.resolve("portfolio_uploadfile")).resolve("15.pdf"), new byte[] { 1 });
        Files.write(Files.createDirectories(publicRoot.resolve("post_uploadfile")).resolve("sample.png"), new byte[] { 2 });
        Files.write(Files.createDirectories(publicRoot.resolve("profile_uploadfile")).resolve("sample.png"), new byte[] { 3 });

        var servlet = new MockServletContext();
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(servlet);
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-storage",
                    Map.of("file.upload.base-dir", publicRoot.toString())));
            context.register(ResourceTestConfiguration.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context).build();
            assertEquals(404, mvc.perform(get("/uploads/portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(404, mvc.perform(head("/uploads/portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(404, mvc.perform(get("/uploads/profile_uploadfile/../portfolio_uploadfile/15.pdf")).andReturn().getResponse().getStatus());
            assertEquals(200, mvc.perform(get("/uploads/post_uploadfile/sample.png")).andReturn().getResponse().getStatus());
            assertEquals(200, mvc.perform(get("/uploads/profile_uploadfile/sample.png")).andReturn().getResponse().getStatus());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(WebConfiguration.class)
    static class ResourceTestConfiguration {
        @Bean
        LoginUserArgumentResolver loginUserArgumentResolver() {
            return new LoginUserArgumentResolver(null, null);
        }
    }
}
