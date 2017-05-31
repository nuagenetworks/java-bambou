package net.nuagenetworks.bambou.jms;

import java.util.HashMap;

import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicSession;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQTopicConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.nuagenetworks.bambou.RestException;

public class RestPushCenterJmsDirectJBoss extends RestPushCenterJms {

    private final static String JMS_USER = "jmsuser@system";
    private final static String JMS_PASSWORD = "jmspass";
    private final static int DEFAULT_MESSAGING_PORT = 5445;
    private final static int NETTY_DEFAULT_CLIENT_FAILURE_CHECK_PERIOD = 10;
    private final static long NETTY_DEFAULT_CONNECTION_TTL = 60000;
    private final static int NETTY_DEFAULT_RECONNECT_ATTEMPTS = -1;
    private final static int NETTY_DEFAULT_RETRY_INTERVAL = 10000;
    private final static int NETTY_DEFAULT_MAX_RETRY_INTERVAL = 60000;

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJmsDirectJBoss.class);

    private boolean haMode;
    private Object waitObj = new Object();

    public void setHaMode(boolean haMode) {
        this.haMode = haMode;
    }

    public RestPushCenterJmsDirectJBoss() {
        jmsHost = null;
        jmsPort = DEFAULT_MESSAGING_PORT;
        jmsUser = JMS_USER;
        jmsPassword = JMS_PASSWORD;
        jmsTopic = JMS_TOPIC;
        haMode = false;
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
                TransportConfiguration transportConfig = new TransportConfiguration(NettyConnectorFactory.class.getName(), transportConfigParams);
                HornetQTopicConnectionFactory topicConnectionFactory = new HornetQTopicConnectionFactory(haMode, transportConfig);
                topicConnectionFactory.setClientFailureCheckPeriod(NETTY_DEFAULT_CLIENT_FAILURE_CHECK_PERIOD);
                topicConnectionFactory.setConnectionTTL(NETTY_DEFAULT_CONNECTION_TTL);
                topicConnectionFactory.setReconnectAttempts(NETTY_DEFAULT_RECONNECT_ATTEMPTS);
                topicConnectionFactory.setRetryInterval(NETTY_DEFAULT_RETRY_INTERVAL);
                topicConnectionFactory.setMaxRetryInterval(NETTY_DEFAULT_MAX_RETRY_INTERVAL);

                // Create the JMS topic connection and start it
                topicConnection = topicConnectionFactory.createTopicConnection(jmsUser, jmsPassword);
                topicConnection.start();

                // Debug
                logger.debug("Subscribing to JMS topic: " + jmsTopic);

                // Create the subscriber
                TopicSession topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
                String[] topicSubNames = jmsTopic.split("/");
                String topicName = topicSubNames[topicSubNames.length - 1];
                Topic topic = topicSession.createTopic(topicName);
                createSubscriber(topicSession, topic);

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

        super.stop();
    }
}
