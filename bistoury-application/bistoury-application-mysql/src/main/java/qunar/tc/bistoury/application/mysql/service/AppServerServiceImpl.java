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

package qunar.tc.bistoury.application.mysql.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import qunar.tc.bistoury.application.api.AppServerService;
import qunar.tc.bistoury.application.api.pojo.AppServer;
import qunar.tc.bistoury.application.mysql.dao.AppServerDao;
import qunar.tc.bistoury.application.mysql.utils.UUIDUtil;
import qunar.tc.bistoury.common.JacksonSerializer;
import qunar.tc.bistoury.serverside.common.ZKClient;
import qunar.tc.bistoury.serverside.common.ZKClientCache;
import qunar.tc.bistoury.serverside.store.RegistryStore;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author leix.xie
 * @date 2019/7/2 15:14
 * @describe
 */
@Service
public class AppServerServiceImpl implements AppServerService {

    private static final Logger logger = LoggerFactory.getLogger(AppServerServiceImpl.class);

    @Autowired
    private AppServerDao appServerDao;

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

    /**
     * 是否开启zknode管理模式
     */
    private boolean enableZkNode = true;


    @Override
    public List<AppServer> getAppServerByAppCode(final String appCode) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appCode), "app code cannot be null or empty");

        if(enableZkNode){
            return getAppServerByAppCodeFromAgent(appCode);
        }
        return this.appServerDao.getAppServerByAppCode(appCode);
    }


    private List<AppServer> getAppServerByAppCodeFromAgent(String appCode){
        List<AppServer> appServers = new ArrayList<>();
        String parentPath = registryStore.getAgentZkPath() + "/" + appCode;
        try {

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
        }catch (KeeperException.NoNodeException ex){
            logger.error("getAppServerByAppCodeFromAgent error zk node :{} not exist",parentPath);
        } catch(Exception e) {
            logger.error("getAppServerByAppCodeFromAgent error", e);
            return ImmutableList.of();
        }
        return appServers;
    }

    @Override
    public int changeAutoJMapHistoEnable(final String serverId, final boolean enable, String loginUser) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverId), "serverId cannot be null or empty");
        logger.info("{} change {}'s jmap switch", loginUser, serverId);
        return this.appServerDao.changeAutoJMapHistoEnable(serverId, enable);
    }

    @Override
    public int changeAutoJStackEnable(final String serverId, final boolean enable, String loginUser) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverId), "serverId cannot be null or empty");
        logger.info("{} change {}'s jsatck switch", loginUser, serverId);
        return this.appServerDao.changeAutoJStackEnable(serverId, enable);
    }

    @Override
    public int deleteAppServerByServerId(final String serverId, String loginUser) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(serverId), "serverId cannot be null or empty");
        logger.info("{} delete a server, serverId: {}", loginUser, serverId);
        if(enableZkNode){
            return 0;//只模式下不支持删除
        }
        return this.appServerDao.deleteAppServerByServerId(serverId);
    }

    @Override
    public int saveAppServer(AppServer appServer, String loginUser) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appServer.getAppCode()), "app code cannot be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appServer.getIp()), "ip cannot be null or empty");
        Preconditions.checkArgument(appServer.getPort() > 0, "port cannot be less than 0");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appServer.getLogDir()), "log dir cannot be null or empty");
        if (Strings.isNullOrEmpty(appServer.getServerId())) {
            logger.info("{} add a server {}", loginUser, appServer);
            appServer.setServerId(UUIDUtil.generateUniqueId());
            AppServer oldAppServer = this.appServerDao.getAppServerByIp(appServer.getIp());
            if (null == oldAppServer) {
                return this.appServerDao.addAppServer(appServer);
            } else {
                throw new RuntimeException("IP地址与" + oldAppServer.getAppCode() + "的IP地址冲突，主机添加失败");
            }
        } else {
            logger.info("{} update appserver {}", loginUser, appServer);
            return this.appServerDao.updateAppServer(appServer);
        }
    }

	@Override
	public AppServer getAppServerByIp(String ip) {
		return this.appServerDao.getAppServerByIp(ip);
	}
}
