package qunar.tc.bistoury.ui.service;

import java.util.List;

public interface AgentService {

    List<String> getApplications();

    List<String> getAppAgents(String applicationName);


}
