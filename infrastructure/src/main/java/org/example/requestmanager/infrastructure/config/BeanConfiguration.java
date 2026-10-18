package org.example.requestmanager.infrastructure.config;

import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BeanConfiguration {

    @Bean
    TransitionPolicy transitionPolicy() {
        return new DiagramTransitionPolicy();
    }
}
