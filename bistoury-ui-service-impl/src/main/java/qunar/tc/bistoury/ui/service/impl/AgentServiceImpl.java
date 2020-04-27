package qunar.tc.bistoury.ui.service.impl;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qunar.tc.bistoury.application.api.pojo.AppServer;
import qunar.tc.bistoury.common.JacksonSerializer;
import qunar.tc.bistoury.serverside.common.ZKClient;
import qunar.tc.bistoury.serverside.common.ZKClientCache;
import qunar.tc.bistoury.serverside.store.RegistryStore;
import qunar.tc.bistoury.ui.service.AgentService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
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
    public List<AppServer> getAppServerByAppCode(String appCode){
        List<AppServer> appServers = new ArrayList<>();
        try {
            String parentPath = registryStore.getAgentZkPath() + "/" + appCode;
            List<String> childrens =  zkClient.getChildren(parentPath);
            if(childrens!=null && !childrens.isEmpty()){
                for(String str:childrens){
                    byte[] value = zkClient.getValue(parentPath + "/" + str);
                    if(value!=null && value.length > 0){
                        appServers.add(JacksonSerializer.deSerialize(value,AppServer.class));
                    }
                }
                return appServers;
            }
        } catch (Exception e) {
            logger.error("get all proxy server address error", e);
            return ImmutableList.of();
        }
        return appServers;
    }
}
