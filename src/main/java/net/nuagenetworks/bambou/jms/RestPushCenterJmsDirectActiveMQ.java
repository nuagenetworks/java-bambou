package net.nuagenetworks.bambou.jms;

import javax.jms.JMSException;
import javax.jms.Topic;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.nuagenetworks.bambou.RestException;

public class RestPushCenterJmsDirectActiveMQ extends RestPushCenterJms {

    private final static String JMS_USER = "csproot@csp";
    private final static String JMS_PASSWORD = "csproot";
    private final static String BROKER_URL_FMT = "tcp://%s:%d?wireFormat.cacheEnabled=false&wireFormat.tightEncodingEnabled=false";
    private final static int JMS_PORT = 61616;

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterJmsDirectActiveMQ.class);

    public RestPushCenterJmsDirectActiveMQ() {
        jmsHost = null;
        jmsPort = JMS_PORT;
        jmsUser = JMS_USER;
        jmsPassword = JMS_PASSWORD;
        jmsTopic = JMS_TOPIC;
    }

    public synchronized void start() throws RestException {
        try {

            // Debug
            logger.debug("Creating JMS connection using host: " + jmsHost + " user: " + jmsUser + " passwd: " + jmsPassword);

            // Create topic connection factory - connecting directly to VSD,
            // bypassing JNDI factory
            String brokerUrl = String.format(BROKER_URL_FMT, jmsHost, jmsPort);
            TopicConnectionFactory topicConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);

            // Create the JMS topic connection and start it
            topicConnection = topicConnectionFactory.createTopicConnection(jmsUser, jmsPassword);
            topicConnection.start();

            // Debug
            logger.debug("Subscribing to JMS topic: " + jmsTopic);

            // Create the subscriber
            TopicSession topicSession = topicConnection.createTopicSession(false, TopicSession.AUTO_ACKNOWLEDGE);
            String[] topicSubNames = jmsTopic.split("/");
            String topicName = topicSubNames[topicSubNames.length - 2] + '/' + topicSubNames[topicSubNames.length - 1];
            Topic topic = topicSession.createTopic(topicName);
            createSubscriber(topicSession, topic);

            // Debug
            logger.info("JMS connection started");
        } catch (JMSException ex) {
            throw new RestException(ex);
        }
    }
}
