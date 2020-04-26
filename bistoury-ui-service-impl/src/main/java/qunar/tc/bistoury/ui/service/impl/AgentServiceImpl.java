package qunar.tc.bistoury.ui.service.impl;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qunar.tc.bistoury.serverside.common.ZKClient;
import qunar.tc.bistoury.serverside.common.ZKClientCache;
import qunar.tc.bistoury.serverside.store.RegistryStore;
import qunar.tc.bistoury.ui.service.AgentService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;

public class AgentServiceImpl implements AgentService {


    private static final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Resource
    private RegistryStore registryStore;

    private ZKClient zkClient;

    @PostConstruct
    public void init() {
        zkClient = ZKClientCache.get(registryStore.getZkAddress());
    }


    @PreDestroy
    public void stop(){
        zkClient.close();
    }

    @Override
    public List<String> getApplications() {
        try {
            return zkClient.getChildren(registryStore.getAgentZkPath());
        } catch (Exception e) {
            logger.error("get all proxy server address error", e);
            return ImmutableList.of();
        }
    }

    @Override
    public List<String> getAppAgents(String applicationName) {
        try {
            return zkClient.getChildren(registryStore.getAgentZkPath() + "/" + applicationName);
        } catch (Exception e) {
            logger.error("get all proxy server address error", e);
            return ImmutableList.of();
        }
    }
}
