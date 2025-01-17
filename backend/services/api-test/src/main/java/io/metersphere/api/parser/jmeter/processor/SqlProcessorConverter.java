package io.metersphere.api.parser.jmeter.processor;

import io.metersphere.api.dto.ApiParamConfig;
import io.metersphere.api.parser.jmeter.constants.JmeterAlias;
import io.metersphere.api.parser.jmeter.constants.JmeterProperty;
import io.metersphere.plugin.api.dto.ParameterConfig;
import io.metersphere.project.api.KeyValueParam;
import io.metersphere.project.api.processor.SQLProcessor;
import io.metersphere.project.constants.ScriptLanguageType;
import io.metersphere.project.dto.environment.EnvironmentInfoDTO;
import io.metersphere.project.dto.environment.datasource.DataSource;
import io.metersphere.sdk.util.LogUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.protocol.jdbc.config.DataSourceElement;
import org.apache.jmeter.protocol.jdbc.processor.AbstractJDBCProcessor;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;

import java.util.List;

import static io.metersphere.api.parser.jmeter.constants.JmeterAlias.TEST_BEAN_GUI;


/**
 * @Author: jianxing
 * @CreateTime: 2023-12-26  14:49
 */
public abstract class SqlProcessorConverter extends MsProcessorConverter<SQLProcessor> {

    public <T extends AbstractJDBCProcessor> void parse(HashTree hashTree,
                                                        SQLProcessor sqlProcessor,
                                                        ParameterConfig config,
                                                        Class<T> jdbcProcessorClass) {
        if (!needParse(sqlProcessor, config)) {
            return;
        }

        ApiParamConfig apiParamConfig = (ApiParamConfig) config;
        EnvironmentInfoDTO envConfig = apiParamConfig.getEnvConfig(sqlProcessor.getProjectId());
        DataSource dataSource = getDataSource(sqlProcessor, envConfig);
        if (dataSource == null) {
            return;
        }
        // 添加数据源
        DataSourceElement dataSourceElement = getDataSourceElement(dataSource);
        hashTree.add(dataSourceElement);

        try {
            // 添加前后置处理器
            T jdbcProcessor = jdbcProcessorClass.getDeclaredConstructor().newInstance();
            getJdbcProcessor(sqlProcessor, jdbcProcessor, dataSource);
            hashTree.add(jdbcProcessor);
        } catch (Exception e) {
            LogUtils.error(e);
        }

        List<KeyValueParam> extractParams = sqlProcessor.getExtractParams()
                .stream()
                .filter(KeyValueParam::isValid)
                .toList();
        // 添加提取的变量
        JSR223PostProcessor jdbcPostProcessor = getJdbcPostProcessor(sqlProcessor.getName(), extractParams);
        if (jdbcPostProcessor != null ) {
            hashTree.add(jdbcPostProcessor);
        }
    }

    public JSR223PostProcessor getJdbcPostProcessor(String name, List<KeyValueParam> extractParams) {
        if (CollectionUtils.isNotEmpty(extractParams)) {
            JSR223PostProcessor jsr223PostProcessor = new JSR223PostProcessor();
            jsr223PostProcessor.setName(name);
            jsr223PostProcessor.setProperty(TestElement.TEST_CLASS, jsr223PostProcessor.getClass().getSimpleName());
            jsr223PostProcessor.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass(JmeterAlias.TEST_BEAN_GUI));
            jsr223PostProcessor.setProperty(JmeterProperty.SCRIPT_LANGUAGE, ScriptLanguageType.BEANSHELL.name().toLowerCase());
            StringBuilder scriptBuilder = new StringBuilder();
            extractParams.stream().filter(KeyValueParam::isValid)
                    .forEach(keyValue -> {
                                String script = """
                                        vars.put("%s","${%s}");
                                        """;
                                scriptBuilder.append(String.format(script, keyValue.getKey(), keyValue.getValue()));
                            });
            jsr223PostProcessor.setProperty(JmeterProperty.SCRIPT, scriptBuilder.toString());
            return jsr223PostProcessor;
        }
        return null;
    }

    protected DataSource getDataSource(SQLProcessor sqlProcessor, EnvironmentInfoDTO envConfig) {
        if (envConfig == null) {
            return null;
        }
        List<DataSource> dataSources = envConfig.getConfig().getDataSources();

        // 先按ID匹配
        dataSources = dataSources.stream()
                .filter(item -> StringUtils.equals(item.getId(), sqlProcessor.getDataSourceId()))
                .toList();

        // 再按名称匹配
        if (CollectionUtils.isEmpty(dataSources)) {
            dataSources = dataSources.stream()
                    .filter(item -> StringUtils.equals(item.getDataSource(), sqlProcessor.getDataSourceName()))
                    .toList();
        }

        return CollectionUtils.isEmpty(dataSources) ? null : dataSources.get(0);
    }

    protected AbstractJDBCProcessor getJdbcProcessor(SQLProcessor sqlProcessor, AbstractJDBCProcessor jdbcProcessor, DataSource dataSource) {
        jdbcProcessor.setEnabled(sqlProcessor.getEnable());
        jdbcProcessor.setName(sqlProcessor.getName() == null ? jdbcProcessor.getClass().getSimpleName() : sqlProcessor.getName());
        jdbcProcessor.setProperty(TestElement.TEST_CLASS, jdbcProcessor.getClass().getSimpleName());
        jdbcProcessor.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass(TEST_BEAN_GUI));
        jdbcProcessor.setDataSource(sqlProcessor.getName());
        jdbcProcessor.setProperty("dataSource", dataSource.getDataSource());
        jdbcProcessor.setProperty("query", sqlProcessor.getScript());
        jdbcProcessor.setProperty("queryTimeout", String.valueOf(sqlProcessor.getQueryTimeout()));
        jdbcProcessor.setProperty("resultVariable", sqlProcessor.getResultVariable());
        jdbcProcessor.setProperty("variableNames", sqlProcessor.getVariableNames());
        jdbcProcessor.setProperty("resultSetHandler", "Store as String");
        jdbcProcessor.setProperty("queryType", "Callable Statement");
        return jdbcProcessor;
    }

    public DataSourceElement getDataSourceElement(DataSource dataSource) {
        DataSourceElement dataSourceElement = new DataSourceElement();
        dataSourceElement.setEnabled(true);
        dataSourceElement.setName(dataSource.getDataSource() + "_JDBCDataSource");
        dataSourceElement.setProperty(TestElement.TEST_CLASS, DataSourceElement.class.getName());
        dataSourceElement.setProperty(TestElement.GUI_CLASS, SaveService.aliasToClass(TEST_BEAN_GUI));
        dataSourceElement.setProperty("autocommit", true);
        dataSourceElement.setProperty("keepAlive", true);
        dataSourceElement.setProperty("preinit", false);
        dataSourceElement.setProperty("dataSource", dataSource.getDataSource());
        dataSourceElement.setProperty("dbUrl", dataSource.getDbUrl());
        dataSourceElement.setProperty("driver", dataSource.getDriver());
        dataSourceElement.setProperty("username", dataSource.getUsername());
        dataSourceElement.setProperty("password", dataSource.getPassword());
        dataSourceElement.setProperty("poolMax", dataSource.getPoolMax());
        dataSourceElement.setProperty("timeout", String.valueOf(dataSource.getTimeout()));
        dataSourceElement.setProperty("connectionAge", 5000);
        dataSourceElement.setProperty("trimInterval", 6000);
        dataSourceElement.setProperty("transactionIsolation", "DEFAULT");
        return dataSourceElement;
    }
}
