/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.lifecycle.internal.operate;

import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobOperateAPI;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.google.common.base.Optional;

import java.util.Collection;

/**
 * 操作作业的实现类.
 *
 * @author zhangliang
 */
public final class JobOperateAPIImpl implements JobOperateAPI {
    
    private final CoordinatorRegistryCenter regCenter;
    
    private final JobOperateTemplate jobOperatorTemplate;
    
    public JobOperateAPIImpl(final CoordinatorRegistryCenter regCenter) {
        this.regCenter = regCenter;
        jobOperatorTemplate = new JobOperateTemplate(regCenter);
    }
    
    @Override
    public void trigger(final Optional<String> jobName, final Optional<String> serverIp, final Optional<String> serverInstanceId) {
        jobOperatorTemplate.operate(jobName, serverIp, serverInstanceId, new JobOperateCallback() {
            
            @Override
            public boolean doOperate(final String jobName, final String serverIp, final String serverInstanceId) {
                regCenter.persist(new JobNodePath(jobName).getServerInstanceNodePath(serverIp, serverInstanceId, JobNodePath.TRIGGER_NODE), "");
                return true;
            }
        });
    }
    
    @Override
    public void disable(final Optional<String> jobName, final Optional<String> serverIp, final Optional<String> serverInstanceId) {
        jobOperatorTemplate.operate(jobName, serverIp, serverInstanceId, new JobOperateCallback() {
            
            @Override
            public boolean doOperate(final String jobName, final String serverIp, final String serverInstanceId) {
                regCenter.persist(new JobNodePath(jobName).getServerInstanceNodePath(serverIp, serverInstanceId, JobNodePath.DISABLED_NODE), "");
                return true;
            }
        });
    }
    
    @Override
    public void enable(final Optional<String> jobName, final Optional<String> serverIp, final Optional<String> serverInstanceId) {
        jobOperatorTemplate.operate(jobName, serverIp, serverInstanceId, new JobOperateCallback() {
            
            @Override
            public boolean doOperate(final String jobName, final String serverIp, final String serverInstanceId) {
                regCenter.remove(new JobNodePath(jobName).getServerInstanceNodePath(serverIp, serverInstanceId, JobNodePath.DISABLED_NODE));
                return true;
            }
        });
    }
    
    @Override
    public void shutdown(final Optional<String> jobName, final Optional<String> serverIp, final Optional<String> serverInstanceId) {
        jobOperatorTemplate.operate(jobName, serverIp, serverInstanceId, new JobOperateCallback() {
            
            @Override
            public boolean doOperate(final String jobName, final String serverIp, final String serverInstanceId) {
                regCenter.persist(new JobNodePath(jobName).getServerInstanceNodePath(serverIp, serverInstanceId, JobNodePath.SHUTDOWN_NODE), "");
                return true;
            }
        });
    }
    
    @Override
    public Collection<String> remove(final Optional<String> jobName, final Optional<String> serverIp, final Optional<String> serverInstanceId) {
        return jobOperatorTemplate.operate(jobName, serverIp, serverInstanceId, new JobOperateCallback() {
            
            @Override
            public boolean doOperate(final String jobName, final String serverIp, final String serverInstanceId) {
                JobNodePath jobNodePath = new JobNodePath(jobName);
                if (regCenter.isExisted(jobNodePath.getServerInstanceNodePath(serverIp, serverInstanceId, JobNodePath.STATUS_NODE)) 
                        || (serverIp + "_" + serverInstanceId).equals(regCenter.get(jobNodePath.getLeaderHostNodePath()))) {
                    return false;
                }
                regCenter.remove(jobNodePath.getServerInstanceNodePath(serverIp, serverInstanceId));
                if (0 == regCenter.getNumChildren(jobNodePath.getServerNodePath())) {
                    regCenter.remove("/" + jobName);
                }
                return true;
            }
        });
    }
}