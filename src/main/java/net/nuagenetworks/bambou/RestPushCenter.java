package net.nuagenetworks.bambou;

public interface RestPushCenter {
    void setUrl(String urlStr);

    void addListener(RestPushCenterListener listener);

    void removeListener(RestPushCenterListener listener);

    void start() throws RestException;

    void stop();
}
