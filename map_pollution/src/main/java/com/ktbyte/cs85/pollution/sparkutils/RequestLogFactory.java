package com.ktbyte.cs85.pollution.sparkutils;

import org.slf4j.Logger;
import org.eclipse.jetty.server.AbstractNCSARequestLog;

import java.io.IOException;

public class RequestLogFactory {

    private Logger logger;

    public RequestLogFactory(org.slf4j.Logger logger2) {
        this.logger = logger2;
    }

    AbstractNCSARequestLog create() {
        return new AbstractNCSARequestLog() {
            @Override
            protected boolean isEnabled() {
                return true;
            }

            @Override
            public void write(String s) throws IOException {
                logger.info(s);
            }
        };
    }
}