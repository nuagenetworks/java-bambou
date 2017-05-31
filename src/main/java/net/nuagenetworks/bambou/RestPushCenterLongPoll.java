/*
  Copyright (c) 2015, Alcatel-Lucent Inc
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.
      * Neither the name of the copyright holder nor the names of its contributors
        may be used to endorse or promote products derived from this software without
        specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package net.nuagenetworks.bambou;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;

import net.nuagenetworks.bambou.model.Events;

public class RestPushCenterLongPoll implements RestPushCenter {

    private static final Logger logger = LoggerFactory.getLogger(RestPushCenterLongPoll.class);

    private static final int SLEEP_PERIOD_IN_MILLIS = 2000;

    private String url;
    private boolean stopPollingEvents;
    private boolean isRunning;
    private RestSession<?> session;
    private List<RestPushCenterListener> listeners = new ArrayList<RestPushCenterListener>();
    private ExecutorService executor;
    private Future<Void> pollingTaskFuture;

    protected RestPushCenterLongPoll(RestSession<?> session) {
        this.session = session;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public synchronized void start() {
        // Don't go any further if polling task is already running
        if (isRunning) {
            return;
        }

        // Create a new thread pool
        executor = Executors.newFixedThreadPool(1);

        // Clear the stop polling notification
        stopPollingEvents = false;

        // Start execution of polling task
        pollingTaskFuture = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                pollEvents();
                return null;
            }
        });

        // Polling is now running
        isRunning = true;

        // Debug
        logger.info("Polling running");
    }

    public synchronized void stop() {
        // Don't go any further if polling task isn't running
        if (!isRunning) {
            return;
        }

        // Debug
        logger.info("Stopping polling. Waiting for blocking REST call to return");

        // Notify polling task to stop
        stopPollingEvents = true;

        try {
            // Wait for polling task to stop
            pollingTaskFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
        }

        // Shutdown thread pool
        executor.shutdown();
        executor = null;

        // Polling task completed
        isRunning = false;
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

    private void pollEvents() {
        String uuid = null;
        while (!stopPollingEvents) {
            try {
                // Debug
                logger.debug("Polling events from VSD using uuid=" + uuid);

                // Get the next events
                ResponseEntity<Events> response = sendRequest(uuid);
                Events events = (Events) response.getBody();
                if (events == null) {
                    // No events, poll again
                    continue;
                }

                if (stopPollingEvents) {
                    break;
                }

                // Debug
                logger.debug("Received events: " + events);

                // Process the events received
                for (JsonNode event : events.getEvents()) {
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

                // Get the next UUID to query for
                uuid = events.getUuid();
            } catch (Exception ex) {
                // Error
                logger.error("Error", ex);

                // Pause and try again
                try {
                    Thread.sleep(SLEEP_PERIOD_IN_MILLIS);
                } catch (InterruptedException e) {
                }
            }
        }

        // Debug
        logger.info("Polling stopped");
    }

    private ResponseEntity<Events> sendRequest(String uuid) throws RestException {
        // Build the url and query parameter for the request
        String eventsUrl = String.format("%s/events", url);
        String params = (uuid != null) ? "uuid=" + uuid : null;

        ResponseEntity<Events> response = null;
        try {
            // Send poll request to server
            response = session.sendRequestWithRetry(HttpMethod.GET, eventsUrl, params, null, null, Events.class);
        } catch (RestStatusCodeException ex) {
            // In case of a 400/Bad Request: re-send request without uuid in
            // order to get a new one
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                response = sendRequest(null);
            } else {
                throw ex;
            }
        }

        return response;
    }
}
