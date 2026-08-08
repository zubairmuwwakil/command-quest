package ca.zubairm.command_quest.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The web entry point.
 *
 * ca.zubairm.command_quest.App remains the console entry point and is entirely
 * unaffected by this class. Two front ends over one domain is the evidence
 * that the Command abstraction is real rather than decorative.
 */
@SpringBootApplication
public class CommandQuestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommandQuestApplication.class, args);
    }

    /**
     * The front end is served from GitHub Pages and the API from a container
     * host, so they are different origins and the browser demands CORS.
     *
     * Listed explicitly rather than "*": this API takes a folder tree from the
     * caller, and there is no reason for arbitrary sites to be posting to it.
     */
    @Bean
    WebMvcConfigurer corsConfiguration(
            @org.springframework.beans.factory.annotation.Value(
                    "${commandquest.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}")
            String[] allowedOrigins) {

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST");
            }
        };
    }
}
