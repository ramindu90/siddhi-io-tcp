/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.extension.siddhi.io.tcp.source;

import org.wso2.extension.siddhi.io.tcp.transport.callback.StreamListener;
import org.wso2.extension.siddhi.io.tcp.transport.config.ServerConfig;
import org.wso2.extension.siddhi.io.tcp.transport.utils.Constant;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.exception.ConnectionUnavailableException;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.stream.input.source.Source;
import org.wso2.siddhi.core.stream.input.source.SourceEventListener;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.core.util.transport.OptionHolder;

import java.util.Arrays;
import java.util.Map;

/**
 * Tcp source extension.
 */
@Extension(
        name = "tcp",
        namespace = "source",
        description = "TBD",
        examples = @Example(description = "TBD", syntax = "TBD")
)
public class TCPSource extends Source {

    private static final String RECEIVER_THREADS = "receiver.threads";
    private static final String WORKER_THREADS = "worker.threads";
    private static final String PORT = "port";
    private static final String HOST = "host";
    private static final String TCP_NO_DELAY = "tcp.no.delay";
    private static final String KEEP_ALIVE = "keep.alive";
    private static final String CONTEXT = "context";
    private SourceEventListener sourceEventListener;
    private String context;
    private ServerConfig serverConfig;


    @Override
    public void init(SourceEventListener sourceEventListener, OptionHolder optionHolder,
                     String[] requestedTransportProperties, ConfigReader configReader,
                     SiddhiAppContext siddhiAppContext) {
        if (requestedTransportProperties != null && requestedTransportProperties.length > 0) {
            throw new SiddhiAppCreationException("'tcp' source does not support requestedTransportProperties," +
                    " but at stream '" + getStreamDefinition().getId() + "' '" +
                    Arrays.deepToString(requestedTransportProperties) +
                    "' transport properties are requested");
        }
        this.sourceEventListener = sourceEventListener;
        context = optionHolder.validateAndGetStaticValue(CONTEXT,
                siddhiAppContext.getName() + "/" + sourceEventListener.getStreamDefinition().getId());

        serverConfig = new ServerConfig();
        serverConfig.setHost(configReader.readConfig(HOST, Constant.DEFAULT_HOST));
        serverConfig.setPort(Integer.parseInt(configReader.readConfig(PORT, "" + Constant.DEFAULT_PORT)));
        serverConfig.setKeepAlive(Boolean.parseBoolean((configReader.readConfig(KEEP_ALIVE, "" + Constant
                .DEFAULT_KEEP_ALIVE))));
        serverConfig.setTcpNoDelay(Boolean.parseBoolean((configReader.readConfig(TCP_NO_DELAY, "" + Constant
                .DEFAULT_TCP_NO_DELAY))));
        serverConfig.setReceiverThreads(Integer.parseInt((configReader.readConfig(RECEIVER_THREADS, "" + Constant
                .DEFAULT_RECEIVER_THREADS))));
        serverConfig.setWorkerThreads(Integer.parseInt((configReader.readConfig(WORKER_THREADS, "" + Constant
                .DEFAULT_WORKER_THREADS))));
    }

    @Override
    public void connect(ConnectionCallback connectionCallback) throws ConnectionUnavailableException {
        TCPServer.getInstance().addStreamListener(new StreamListener() {
            @Override
            public String getChannelId() {
                return context;
            }

            @Override
            public void onMessage(byte[] message) {
                sourceEventListener.onEvent(message, null);
            }
        });
        TCPServer.getInstance().start(serverConfig);
    }

    @Override
    public Class[] getOutputEventClasses() {
        return new Class[]{byte[].class};
    }

    @Override
    public void disconnect() {
        TCPServer.getInstance().removeStreamListener(context);
        TCPServer.getInstance().stop();
    }

    @Override
    public void destroy() {
    }

    @Override
    public void pause() {
        TCPServer.getInstance().pause();
    }

    @Override
    public void resume() {
        TCPServer.getInstance().resume();
    }

    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    @Override
    public void restoreState(Map<String, Object> state) {
        //no state
    }
}
