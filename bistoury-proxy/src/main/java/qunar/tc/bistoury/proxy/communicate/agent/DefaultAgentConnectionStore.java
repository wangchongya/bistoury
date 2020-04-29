/*
 * Copyright (C) 2019 Qunar, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package qunar.tc.bistoury.proxy.communicate.agent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import qunar.tc.bistoury.application.api.pojo.AppServer;
import qunar.tc.bistoury.application.mysql.utils.UUIDUtil;
import qunar.tc.bistoury.common.JacksonSerializer;
import qunar.tc.bistoury.proxy.startup.ZkManager;
import qunar.tc.bistoury.serverside.store.RegistryStore;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * @author zhenyu.nie created on 2019 2019/5/13 19:44
 */
@Service
public class DefaultAgentConnectionStore implements AgentConnectionStore {

    private final ConcurrentMap<String, AgentConnection> connections = Maps.newConcurrentMap();

    @Autowired
    private ZkManager zkManager;

    @Autowired
    private RegistryStore registryStore;

    @Override
    public AgentConnection register(String agentId,Map<String,String> prop,int agentVersion, Channel channel) {
        String applicationName = prop.get("applicationName");
        String hostname = prop.get("hostname");
        String configEnv = prop.get("configEnv");

        DefaultAgentConnection agentConnection = new DefaultAgentConnection(agentId,applicationName, agentVersion, channel);
        AgentConnection oldConnection = connections.get(agentId);
        if (!Objects.equals(oldConnection, agentConnection)) {
            oldConnection = connections.put(agentId, agentConnection);
            agentConnection.init();
            AppServer appServer = new AppServer(UUIDUtil.generateUniqueId(),agentId,0,hostname,"/usr/local/yunji/logs",configEnv,applicationName);
            zkManager.createOnlyLastEphemeralNode(registryStore.getAgentZkPath() + "/" + applicationName + "/" + agentId,true, JacksonSerializer.serialize(appServer));
            agentConnection.closeFuture().addListener(() -> {
                connections.remove(agentId, agentConnection);
                zkManager.deleteNode(registryStore.getAgentZkPath() + "/" + applicationName + "/" + agentId );
             }
            , MoreExecutors.directExecutor());
            if (oldConnection != null && !Objects.equals(oldConnection, agentConnection)) {
                oldConnection.close();
            }
            return agentConnection;
        } else {
            return oldConnection;
        }
    }

    @Override
    public Optional<AgentConnection> getConnection(String agentId) {
        AgentConnection agentConnection = connections.get(agentId);
        if (agentConnection != null) {
            return Optional.of(agentConnection);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, AgentConnection> getAgentConnection() {
        return ImmutableMap.copyOf(connections);
    }

    @Override
    public Map<String, AgentConnection> searchConnection(String agentId) {
        Map<String, AgentConnection> connection = getAgentConnection();
        Map<String, AgentConnection> result = Maps.filterKeys(connection, key -> key.indexOf(agentId) >= 0);
        return result;
    }
}
