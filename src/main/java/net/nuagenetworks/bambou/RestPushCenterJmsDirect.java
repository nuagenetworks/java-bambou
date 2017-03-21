package net.nuagenetworks.bambou;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQTopicConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestPushCenterJmsDirect implements RestPushCenter {

    private final static String JMS_TOPIC = "jms/topic/CNAMessages";
    private final static String JMS_USER = "jmsuser@system";
    private final static String JMS_PASSWORD = "jmspass";
    private final static int DEFAULT_MESSAGING_PORT = 5445;
    private final static int NETTY_DEFAULT_CLIENT_FAILURE_CHECK_PERIOD = 10;
    private final static long NETTY_DEFAULT_CONNECTION_TTL = 60000;
    private final static int NETTY_DEFAULT_RECONNECT_ATTEMPTS = -1;
    private final static int NETTY_DEFAULT_RETRY_INTERVAL = 10000;
    private final static int NETTY_DEFAULT_MAX_RETRY_INTERVAL = 60000;

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJmsDirect.class);

    private List<RestPushCenterListener> listeners = new ArrayList<RestPushCenterListener>();
    private String jmsHost;
    private int jmsPort;
    private String jmsUser;
    private String jmsPassword;
    private String jmsTopic;
    private boolean haMode;
    private TopicConnection topicConnection;
    private Object waitObj = new Object();

    public void setHost(String jmsHost) {
        this.jmsHost = jmsHost;
    }

    public void setPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public void setTopic(String jmsTopic) {
        this.jmsTopic = jmsTopic;
    }

    public void setHaMode(boolean haMode) {
        this.haMode = haMode;
    }

    public void setUrl(String urlStr) {
        try {
            URL url = new URL(urlStr);
            jmsHost = url.getHost();
            jmsPort = DEFAULT_MESSAGING_PORT;
            jmsUser = JMS_USER;
            jmsPassword = JMS_PASSWORD;
            jmsTopic = JMS_TOPIC;
            haMode = false;
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

    public void start() throws RestException {
        synchronized (this) {
            try {
                // Debug
                logger.debug("Creating JMS connection using host: " + jmsHost + " user: " + jmsUser + " passwd: " + jmsPassword);

                // Initialize transport configuration parameters
                HashMap<String, Object> transportConfigParams = new HashMap<String, Object>();
                transportConfigParams.put(org.hornetq.core.remoting.impl.netty.TransportConstants.HOST_PROP_NAME, jmsHost);
                transportConfigParams.put(org.hornetq.core.remoting.impl.netty.TransportConstants.PORT_PROP_NAME, jmsPort);

                // Create transport configuration - connecting directly to VSD,
                // bypassing JNDI factory
                TransportConfiguration tc = new TransportConfiguration(NettyConnectorFactory.class.getName(), transportConfigParams);
                HornetQTopicConnectionFactory tcf = new HornetQTopicConnectionFactory(haMode, tc);
                tcf.setClientFailureCheckPeriod(NETTY_DEFAULT_CLIENT_FAILURE_CHECK_PERIOD);
                tcf.setConnectionTTL(NETTY_DEFAULT_CONNECTION_TTL);
                tcf.setReconnectAttempts(NETTY_DEFAULT_RECONNECT_ATTEMPTS);
                tcf.setRetryInterval(NETTY_DEFAULT_RETRY_INTERVAL);
                tcf.setMaxRetryInterval(NETTY_DEFAULT_MAX_RETRY_INTERVAL);

                // Create the JMS topic connection and start it
                TopicConnection topicConnection = tcf.createTopicConnection(JMS_USER, JMS_PASSWORD);
                topicConnection.start();

                // Debug
                logger.debug("Subscribing to JMS topic: " + jmsTopic);

                // Create the subscriber
                String[] topicName = jmsTopic.split("/");
                TopicSession topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
                Topic topic = topicSession.createTopic(topicName[topicName.length - 1]);
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

                // Debug
                logger.info("JMS connection started");
            } catch (JMSException ex) {
                throw new RestException(ex);
            }
        }

        // Block until the push center is stopped
        synchronized (waitObj) {
            try {
                waitObj.wait();
            } catch (InterruptedException ex) {
                logger.error("Error", ex);
            }
        }
    }

    public void stop() {
        // Unblock start()
        synchronized (waitObj) {
            waitObj.notifyAll();
        }

        synchronized (this) {
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
