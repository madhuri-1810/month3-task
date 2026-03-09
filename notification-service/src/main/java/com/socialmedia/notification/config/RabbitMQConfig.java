package com.socialmedia.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "social-media.exchange";

    // Queue names
    public static final String POST_CREATED_QUEUE = "post.created.queue";
    public static final String USER_FOLLOWED_QUEUE = "user.followed.queue";
    public static final String POST_LIKED_QUEUE = "post.liked.queue";
    public static final String MESSAGE_SENT_QUEUE = "message.sent.queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue postCreatedQueue() {
        return QueueBuilder.durable(POST_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", "social-media.dlx")
                .build();
    }

    @Bean
    public Queue userFollowedQueue() {
        return QueueBuilder.durable(USER_FOLLOWED_QUEUE)
                .withArgument("x-dead-letter-exchange", "social-media.dlx")
                .build();
    }

    @Bean
    public Queue postLikedQueue() {
        return QueueBuilder.durable(POST_LIKED_QUEUE)
                .withArgument("x-dead-letter-exchange", "social-media.dlx")
                .build();
    }

    @Bean
    public Queue messageSentQueue() {
        return QueueBuilder.durable(MESSAGE_SENT_QUEUE)
                .withArgument("x-dead-letter-exchange", "social-media.dlx")
                .build();
    }

    // Bindings: queue ← routing key ← exchange
    @Bean
    public Binding postCreatedBinding() {
        return BindingBuilder.bind(postCreatedQueue())
                .to(exchange())
                .with("post.created");
    }

    @Bean
    public Binding userFollowedBinding() {
        return BindingBuilder.bind(userFollowedQueue())
                .to(exchange())
                .with("user.followed");
    }

    @Bean
    public Binding postLikedBinding() {
        return BindingBuilder.bind(postLikedQueue())
                .to(exchange())
                .with("post.liked");
    }

    @Bean
    public Binding messageSentBinding() {
        return BindingBuilder.bind(messageSentQueue())
                .to(exchange())
                .with("message.sent");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // Dead Letter Exchange for failed messages
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("social-media.dlx");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead-letter.queue").build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead-letter");
    }
}
