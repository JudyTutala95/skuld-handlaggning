package se.jtutala.handlaggning.arende;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/// Skapar Kafka-ämnet `arende-status` när tjänsten startar, om det inte redan finns.
///
/// Tre partitioner gör att flera instanser av en mottagare kan dela på arbetet.
@Configuration(proxyBeanMethods = false)
class KafkaKonfiguration {

    @Bean
    NewTopic arendeStatusAmne() {
        return TopicBuilder.name(ArendeStatusAndrad.AMNE).partitions(3).replicas(1).build();
    }
}
