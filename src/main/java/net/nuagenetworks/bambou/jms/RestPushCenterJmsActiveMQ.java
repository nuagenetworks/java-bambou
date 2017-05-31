package net.nuagenetworks.bambou.jms;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.nuagenetworks.bambou.RestException;

public class RestPushCenterJmsActiveMQ extends RestPushCenterJms {

    private final static String JNDI_FACTORY = "org.apache.activemq.jndi.ActiveMQInitialContextFactory";
    private final static String JMS_FACTORY = "ConnectionFactory";
    private final static String JMS_USER = "csproot@csp";
    private final static String JMS_PASSWORD = "csproot";
    private final static String PROVIDER_URL_FMT = "tcp://%s:%d?wireFormat.cacheEnabled=false&wireFormat.tightEncodingEnabled=false";
    private final static int JMS_PORT = 61616;

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJmsActiveMQ.class);

    private InitialContext context;

    public RestPushCenterJmsActiveMQ() {
        jmsHost = null;
        jmsPort = JMS_PORT;
        jmsUser = JMS_USER;
        jmsPassword = JMS_PASSWORD;
        jmsTopic = JMS_TOPIC;
    }

    public synchronized void start() throws RestException {
        try {
            String jndiProviderUrl = String.format(PROVIDER_URL_FMT, jmsHost, jmsPort);
            String jndiFactory = JNDI_FACTORY;
            String jmsFactory = JMS_FACTORY;

            // Debug
            logger.debug("Creating JNDI connection to: " + jndiProviderUrl + " using factory: " + jndiFactory);

            // Initialize JNDI connection
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
            env.put(Context.PROVIDER_URL, jndiProviderUrl);
            context = new InitialContext(env);

            // Debug
            logger.debug("Creating JMS connection using factory: " + jmsFactory + " user: " + jmsUser + " passwd: " + jmsPassword);

            // Create the JMS topic connection and start it
            TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) context.lookup(jmsFactory);
            topicConnection = topicConnectionFactory.createTopicConnection(jmsUser, jmsPassword);
            topicConnection.start();

            // Debug
            logger.debug("Subscribing to JMS topic: " + jmsTopic);

            // Create the subscriber
            Topic topic = (Topic) context.lookup(jmsTopic);
            TopicSession topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
            createSubscriber(topicSession, topic);

            // Debug
            logger.info("JMS connection started");
        } catch (NamingException | JMSException ex) {
            throw new RestException(ex);
        }
    }

    public synchronized void stop() {
        try {
            // Close JNDI
            if (context != null) {
                context.close();
            }

            super.stop();
        } catch (NamingException ex) {
            logger.error("Error", ex);
        }
    }
}
