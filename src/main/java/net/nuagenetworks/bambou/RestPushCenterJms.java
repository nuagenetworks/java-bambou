package net.nuagenetworks.bambou;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestPushCenterJms implements RestPushCenter {

    private final static String JNDI_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
    private final static String JMS_FACTORY = "jms/RemoteConnectionFactory";
    private final static String JMS_TOPIC = "jms/topic/CNAMessages";
    private final static String JNDI_USER = "vsduser";
    private final static String JNDI_PASSWORD = "vsdpass";
    private final static String JMS_USER = "jmsuser@system";
    private final static String JMS_PASSWORD = "jmspass";

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJms.class);

    private List<RestPushCenterListener> listeners = new ArrayList<RestPushCenterListener>();
    private String jndiProviderUrl;
    private String jmsUser;
    private String jmsPassword;
    private TopicConnection topicConnection;
    private InitialContext context;

    public void setUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            jndiProviderUrl = "remote://" + url.getHost() + ":4447";
            jmsUser = JMS_USER;
            jmsPassword = JMS_PASSWORD;
        } catch (MalformedURLException ex) {
            logger.error("Error", ex);
        }
    }

    public void addListener(RestPushCenterListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(RestPushCenterListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public synchronized void start() throws RestException {
        try {
            String jndiFactory = JNDI_FACTORY;
            String jndiUser = JNDI_USER;
            String jndiPassword = JNDI_PASSWORD;
            String jmsFactory = JMS_FACTORY;
            String jmsTopic = JMS_TOPIC;

            // Debug
            logger.info(
                    "Creating JNDI connection to: " + jndiProviderUrl + " using factory: " + jndiFactory + " user: " + jndiUser + " passwd: " + jndiPassword);

            // Initialize JNDI connection
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
            env.put(Context.PROVIDER_URL, jndiProviderUrl);
            env.put(Context.SECURITY_PRINCIPAL, jndiUser);
            env.put(Context.SECURITY_CREDENTIALS, jndiPassword);
            context = new InitialContext(env);

            // Debug
            logger.info("Creating JMS connection using factory: " + jmsFactory + " user: " + jmsUser + " passwd: " + jmsPassword);

            // Create the JMS topic connection and start it
            TopicConnectionFactory topicConnectionFactory = (TopicConnectionFactory) context.lookup(jmsFactory);
            topicConnection = topicConnectionFactory.createTopicConnection(jmsUser, jmsPassword);
            topicConnection.start();

            // Debug
            logger.info("Subscribing to JMS topic: " + jmsTopic);

            // Create the subscriber
            Topic topic = (Topic) context.lookup(jmsTopic);
            TopicSession topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
            MessageConsumer subscriber = topicSession.createConsumer(topic);

            // Attach message listener to subscriber
            subscriber.setMessageListener(new MessageListener() {
                public void onMessage(javax.jms.Message message) {
                    try {
                        // Process the message
                        processMessage(message);
                    } catch (Exception ex) {
                        // Error
                        logger.error("Error", ex);
                    }
                }
            });
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

            // Close topic connection
            if (topicConnection != null) {
                topicConnection.close();
            }
        } catch (NamingException | JMSException ex) {
            logger.error("Error", ex);
        }
    }

    private void processMessage(Message message) throws Exception {
        // Get the message
        TextMessage text = (TextMessage) message;
        String json = text.getText();

        // Debug
        logger.debug("Processing message: " + json);

        // Parse the content of the message in JSON format => event
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser parser = factory.createParser(json);
        JsonNode event = mapper.readTree(parser);

        // Take a snapshot of the listeners
        List<RestPushCenterListener> listenersSnapshot = null;
        synchronized (listeners) {
            listenersSnapshot = new ArrayList<>(listeners);
        }

        // Notify the listeners
        for (RestPushCenterListener listener : listenersSnapshot) {
            listener.onEvent(event);
        }
    }
}
