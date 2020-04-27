package qunar.tc.bistoury.proxy.startup;

import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import qunar.tc.bistoury.serverside.common.ZKClient;
import qunar.tc.bistoury.serverside.common.ZKClientCache;
import qunar.tc.bistoury.serverside.store.RegistryStore;

import java.util.List;

@Service
public class ZkManager {

    private static final Logger logger = LoggerFactory.getLogger(ZkManager.class);


    @Autowired
    private RegistryStore registryStore;

    private ZKClient zkClient;

    public void start() {
        //集成业务
        zkClient = ZKClientCache.get(registryStore.getZkAddress());

    }

    public void stop() {
        zkClient.close();
    }

    public List<String> getChildren(String path) throws Exception{
        return zkClient.getChildren(path);
    }


    public boolean checkExist(String path){
        return zkClient.checkExist(path);
    }


    /**
     * 创建目前只有最后可选临时节点
     * @param path
     * @param ephemeral
     * @return
     */
    public boolean createOnlyLastEphemeralNode(String path,boolean ephemeral){
        try{
            if (checkExist(path)) {
                return true;
            }

            int i = path.lastIndexOf('/');
            if (i > 0) {
                createOnlyLastEphemeralNode(path.substring(0, i), false);
            }
            if(ephemeral){
                 zkClient.addEphemeralNode(path);
            }else {
                 zkClient.addPersistentNode(path);
            }
            return true;
        }catch (Exception e){
            logger.error("zk addEphemeralNode path error", e);
        }
        return false;
    }


    public boolean deleteNode(String... nodes) {
        boolean ret = true;
        for (String node : nodes) {
            if (node != null) {
                try {
                    zkClient.deletePath(node);
                    logger.info("zk delete successfully, node {}", node);
                } catch (KeeperException.NoNodeException e) {
                    // ignore
                } catch (Exception e) {
                    logger.error("zk delete path error", e);
                    ret = false;
                }
            }
        }
        return ret;
    }

    public String doRegister(String basePath, String node) {
        try {
            createOnlyLastEphemeralNode(basePath,false);
            node = ZKPaths.makePath(basePath, node);
            deleteNode(node);
            zkClient.addEphemeralNode(node);
            logger.info("zk register successfully, node {}", node);
        } catch (Exception e) {
            logger.error("zk register failed", e);
        }
        return node;
    }

    public void addConnectionChangeListener(ConnectionStateListener listener){
        zkClient.addConnectionChangeListener(listener);
    }
}
