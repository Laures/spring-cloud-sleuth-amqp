package com.netshoes.springframework.cloud.sleuth.instrument.amqp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;

/**
 * This abstract Aspect is used to intercept methods annotated with {@link
 * org.springframework.amqp.rabbit.annotation.RabbitListener} and {@link
 * org.springframework.amqp.rabbit.annotation.RabbitHandler} and invoke {@link
 * AmqpMessagingSpanManager#beforeHandle(Message)} with {@link Message}.
 *
 * @author André Ignacio
 * @author Dominik Bartholdi
 * @since 0.9
 */
public abstract class AbstractRabbitMessageReceiverAspect {
  private final AmqpMessagingSpanManager spanManager;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * Creates a new instance.
   *
   * @param spanManager Span manager for AMQP messaging
   */
  public AbstractRabbitMessageReceiverAspect(AmqpMessagingSpanManager spanManager) {
    this.spanManager = spanManager;
  }

  protected Object executeAroundMessageReceive(ProceedingJoinPoint call) throws Throwable {
    final Object result;
    final Object[] args = call.getArgs();
    final Message message = getMessageArgument(args);
    if (message == null) {
      logger.debug("Ignoring execution around {}. Message argument not found.", call);
      result = call.proceed();
    } else {
      spanManager.beforeHandle(message);
      try {
        result = call.proceed();
        spanManager.afterHandle(null);

        return result;
      } catch (Exception e) {
        spanManager.afterHandle(e);
        throw e;
      }
    }
    return result;
  }

  private Message getMessageArgument(Object[] args) {
    for (Object arg : args) {
      if (arg instanceof Message) {
        return (Message) arg;
      }
    }
    return null;
  }
}
