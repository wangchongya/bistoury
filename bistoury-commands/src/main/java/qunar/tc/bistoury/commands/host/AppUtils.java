package qunar.tc.bistoury.commands.host;

import com.taobao.arthas.core.util.StringUtils;

import java.lang.management.RuntimeMXBean;
import java.util.List;

public class AppUtils {

    /**
     * 获取agent应用名
     * @param pid
     * @return
     */
    public static String getAgentAppName(int pid) {

        String applicationName = System.getProperty("applicationName");
        if(StringUtils.isBlank(applicationName)){
            applicationName = System.getProperty("project.name");
        }

        if(StringUtils.isBlank(applicationName)){
            try{
                VirtualMachineUtil.VMConnector  connect = VirtualMachineUtil.connect(pid);
                RuntimeMXBean runtimeBean = connect.getRuntimeMXBean();
                List<String> arguments = runtimeBean.getInputArguments();
                for(String str:arguments){
                    if(str.startsWith("-DapplicationName=") ){
                        return str.replace("-DapplicationName=","");
                    }
                    if(str.startsWith("-Dproject.name=") ){
                        return str.replace("-Dproject.name=","");
                    }
                }
            }catch (Exception ex){

            }
        }
        return applicationName;
    }
}
