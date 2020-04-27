package qunar.tc.bistoury.ui.service;

import qunar.tc.bistoury.application.api.pojo.AppServer;

import java.util.List;

public interface AgentService {

    List<String> getApplications();

    List<AppServer> getAppServerByAppCode(String appCode);


}
