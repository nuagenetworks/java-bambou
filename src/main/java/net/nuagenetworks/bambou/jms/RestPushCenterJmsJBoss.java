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

public class RestPushCenterJmsJBoss extends RestPushCenterJms {

    private final static String JNDI_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
    private final static String JMS_FACTORY = "jms/RemoteConnectionFactory";
    private final static String JNDI_USER = "vsduser";
    private final static String JNDI_PASSWORD = "vsdpass";
    private final static String JMS_USER = "jmsuser@system";
    private final static String JMS_PASSWORD = "jmspass";
    private final static int JMS_PORT = 4447;

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJmsJBoss.class);

    private InitialContext context;

    public RestPushCenterJmsJBoss() {
        jmsHost = null;
        jmsPort = JMS_PORT;
        jmsUser = JMS_USER;
        jmsPassword = JMS_PASSWORD;
        jmsTopic = JMS_TOPIC;
    }

    public synchronized void start() throws RestException {
        try {
            String jndiProviderUrl = "remote://" + jmsHost + ":" + jmsPort;
            String jndiFactory = JNDI_FACTORY;
            String jndiUser = JNDI_USER;
            String jndiPassword = JNDI_PASSWORD;
            String jmsFactory = JMS_FACTORY;

            // Debug
            logger.debug(
                    "Creating JNDI connection to: " + jndiProviderUrl + " using factory: " + jndiFactory + " user: " + jndiUser + " passwd: " + jndiPassword);

            // Initialize JNDI connection
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
            env.put(Context.PROVIDER_URL, jndiProviderUrl);
            env.put(Context.SECURITY_PRINCIPAL, jndiUser);
            env.put(Context.SECURITY_CREDENTIALS, jndiPassword);
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
