package org.example.requestmanager.infrastructure.config;

import org.example.requestmanager.application.port.out.PublicationNumberGenerator;
import org.example.requestmanager.application.port.out.RequestRepository;
import org.example.requestmanager.application.service.RequestService;
import org.example.requestmanager.domain.transition.DiagramTransitionPolicy;
import org.example.requestmanager.domain.transition.TransitionPolicy;
import org.example.requestmanager.infrastructure.transaction.TransactionalRequestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class BeanConfiguration {

    @Bean
    TransitionPolicy transitionPolicy() {
        return new DiagramTransitionPolicy();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * The only bean implementing the use case ports. RequestService is created here
     * rather than registered, so every caller goes through the transactional decorator.
     */
    @Bean
    TransactionalRequestService requestUseCases(RequestRepository repository,
                                                PublicationNumberGenerator publicationNumberGenerator,
                                                TransitionPolicy transitionPolicy,
                                                Clock clock,
                                                PlatformTransactionManager transactionManager) {
        RequestService service = new RequestService(repository, publicationNumberGenerator, transitionPolicy, clock);
        return new TransactionalRequestService(service, service, service, service, service, transactionManager);
    }
}
