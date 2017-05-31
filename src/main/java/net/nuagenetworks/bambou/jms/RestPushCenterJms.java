package net.nuagenetworks.bambou.jms;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.nuagenetworks.bambou.RestPushCenter;
import net.nuagenetworks.bambou.RestPushCenterListener;

public abstract class RestPushCenterJms implements RestPushCenter {

    protected final static String JMS_TOPIC = "jms/topic/CNAMessages";

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJms.class);

    protected String jmsHost;
    protected int jmsPort;
    protected String jmsUser;
    protected String jmsPassword;
    protected String jmsTopic;
    protected TopicConnection topicConnection;
    protected List<RestPushCenterListener> listeners = new ArrayList<RestPushCenterListener>();

    public void setPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public void setHost(String jmsHost) {
        this.jmsHost = jmsHost;
    }

    public void setUser(String jmsUser) {
        this.jmsUser = jmsUser;
    }

    public void setPassword(String jmsPassword) {
        this.jmsPassword = jmsPassword;
    }

    public void setTopic(String jmsTopic) {
        this.jmsTopic = jmsTopic;
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

    public void setUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            jmsHost = url.getHost();
        } catch (MalformedURLException ex) {
            logger.error("Error", ex);
        }
    }

    public synchronized void stop() {
        try {
            // Close topic connection
            if (topicConnection != null) {
                topicConnection.close();
            }

            // Debug
            logger.info("JMS connection stopped");
        } catch (JMSException ex) {
            logger.error("Error", ex);
        }
    }

    protected void createSubscriber(TopicSession topicSession, Topic topic) throws JMSException {
        // Create subscriber
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
    }

    protected void processMessage(Message message) throws Exception {
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
