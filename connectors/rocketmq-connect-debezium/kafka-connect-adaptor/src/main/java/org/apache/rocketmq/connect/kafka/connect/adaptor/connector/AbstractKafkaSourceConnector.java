package org.apache.rocketmq.connect.kafka.connect.adaptor.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.component.task.source.SourceConnector;
import io.openmessaging.connector.api.errors.ConnectException;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.kafka.connect.runtime.ConnectorConfig;
import org.apache.kafka.connect.runtime.TaskConfig;
import org.apache.rocketmq.connect.kafka.connect.adaptor.config.ConnectKeyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * kafka source connector
 */
public abstract class AbstractKafkaSourceConnector extends SourceConnector implements ConnectorClassSetter {


    /**
     * kafka connect init
     */
    protected ConnectKeyValue configValue;

    /**
     * task config
     */
    protected Map<String, String> taskConfig;

    /**
     * source connector
     */
    protected org.apache.kafka.connect.source.SourceConnector sourceConnector;

    /**
     * try override start and stop
     * @return
     */
    protected org.apache.kafka.connect.source.SourceConnector originalSinkConnector(){
        return sourceConnector;
    }

    /**
     * Returns a set of configurations for Tasks based on the current configuration,
     * producing at most count configurations.
     * @param maxTasks maximum number of configurations to generate
     * @return configurations for Tasks
     */
    @Override
    public List<KeyValue> taskConfigs(int maxTasks) {
        List<Map<String, String>> groupConnectors = sourceConnector.taskConfigs(maxTasks);
        List<KeyValue> configs = new ArrayList<>();
        for (Map<String, String> configMaps : groupConnectors) {
            KeyValue keyValue = new DefaultKeyValue();
            configMaps.forEach((k, v)->{
                keyValue.put(k, v);
            });
            configs.add(keyValue);
        }
        return configs;
    }

    /**
     * Start the component
     * @param config component context
     */
    @Override
    public void start(KeyValue config) {
        this.configValue = new ConnectKeyValue();
        config.keySet().forEach(key -> {
            this.configValue.put(key, config.getString(key));
        });
        setConnectorClass(configValue);
       taskConfig = new HashMap<>(configValue.config());
        // get the source class name from config and create source task from reflection
        try {
            sourceConnector = Class.forName(taskConfig.get(ConnectorConfig.CONNECTOR_CLASS_CONFIG))
                    .asSubclass(org.apache.kafka.connect.source.SourceConnector.class)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (Exception e) {
            throw new ConnectException("Load task class failed, " + taskConfig.get(TaskConfig.TASK_CLASS_CONFIG));
        }
    }

    /**
     * Stop the component.
     */
    @Override
    public void stop() {
        if (sourceConnector != null){
            sourceConnector = null;
            configValue =  null;
            this.taskConfig = null;
        }
    }
}