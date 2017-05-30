package net.nuagenetworks.bambou;

public enum RestPushCenterType {
    LONG_POLL,   // REST 
    JMS,         // JMS - JBoss (VSD 4.0.x)
    JMS_DIRECT,  // JMS - JBoss, no JNDI (VSD 4.0.x)
    JMS_ACTIVEMQ // JMS - ActiveMQ (VSD 5.0.x)
};