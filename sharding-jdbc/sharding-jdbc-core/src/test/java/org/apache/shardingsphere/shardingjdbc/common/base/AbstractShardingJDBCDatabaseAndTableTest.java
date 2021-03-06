/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.shardingjdbc.common.base;

import com.google.common.base.Joiner;
import org.apache.shardingsphere.api.config.sharding.KeyGeneratorConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.core.constant.DatabaseType;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.shardingjdbc.fixture.PreciseOrderShardingAlgorithm;
import org.apache.shardingsphere.shardingjdbc.fixture.RangeOrderShardingAlgorithm;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.datasource.ShardingDataSource;
import org.junit.Before;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public abstract class AbstractShardingJDBCDatabaseAndTableTest extends AbstractSQLTest {

    @Before
    public void cleanAndInitTable() {
        importDataSet();
    }

    @Before
    public void initShardingDataSources() throws SQLException {
        if (null != shardingDataSource) {
            return;
        }
        
        Map<DatabaseType, Map<String, DataSource>> dataSourceMap = createDataSourceMap();
        for (Entry<DatabaseType, Map<String, DataSource>> entry : dataSourceMap.entrySet()) {
            final ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
            List<String> orderActualDataNodes = new LinkedList<>();
            for (String dataSourceName : entry.getValue().keySet()) {
                orderActualDataNodes.add(dataSourceName + ".t_order_${0..1}");
            }
            TableRuleConfiguration orderTableRuleConfig = new TableRuleConfiguration("t_order", Joiner.on(",").join(orderActualDataNodes));
            shardingRuleConfig.getTableRuleConfigs().add(orderTableRuleConfig);
            List<String> orderItemActualDataNodes = new LinkedList<>();
            for (String dataSourceName : entry.getValue().keySet()) {
                orderItemActualDataNodes.add(dataSourceName + ".t_order_item_${0..1}");
            }
            TableRuleConfiguration orderItemTableRuleConfig = new TableRuleConfiguration("t_order_item", Joiner.on(",").join(orderItemActualDataNodes));
            orderItemTableRuleConfig.setKeyGeneratorConfig(new KeyGeneratorConfiguration("INCREMENT", "item_id", new Properties()));
            shardingRuleConfig.getTableRuleConfigs().add(orderItemTableRuleConfig);
            TableRuleConfiguration configTableRuleConfig = new TableRuleConfiguration("t_config");
            shardingRuleConfig.getTableRuleConfigs().add(configTableRuleConfig);
            shardingRuleConfig.getBindingTableGroups().add("t_order, t_order_item");
            shardingRuleConfig.setDefaultTableShardingStrategyConfig(new StandardShardingStrategyConfiguration("order_id", new PreciseOrderShardingAlgorithm(), new RangeOrderShardingAlgorithm()));
            shardingRuleConfig.setDefaultDatabaseShardingStrategyConfig(new StandardShardingStrategyConfiguration("user_id", new PreciseOrderShardingAlgorithm(), new RangeOrderShardingAlgorithm()));
            ShardingRule shardingRule = new ShardingRule(shardingRuleConfig, entry.getValue().keySet());
            shardingDataSource = new ShardingDataSource(entry.getValue(), shardingRule);
        }
    }
    
    @Override
    protected final List<String> getInitDataSetFiles() {
        return Arrays.asList("integrate/dataset/jdbc/jdbc_0.xml", "integrate/dataset/jdbc/jdbc_1.xml");
    }
    
    protected final ShardingDataSource getShardingDataSource() {
        return shardingDataSource;
    }
}
