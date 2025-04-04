/*
 *  Copyright 1999-2021 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.alibaba.nacos.core.remote;

import com.alibaba.nacos.api.remote.RemoteConstants;
import com.alibaba.nacos.api.remote.request.HealthCheckRequest;
import com.alibaba.nacos.api.remote.request.ServerReloadRequest;
import com.alibaba.nacos.core.remote.core.ServerReloaderRequestHandler;
import com.alibaba.nacos.plugin.control.ControlManagerCenter;
import com.alibaba.nacos.plugin.control.tps.TpsControlManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * {@link RequestHandlerRegistry} unit test.
 *
 * @author chenglu
 * @date 2021-07-02 19:22
 */
@ExtendWith(MockitoExtension.class)
class RequestHandlerRegistryTest {
    
    @InjectMocks
    private RequestHandlerRegistry registry;
    
    @InjectMocks
    private ContextRefreshedEvent contextRefreshedEvent;
    
    @Mock
    private AnnotationConfigApplicationContext applicationContext;
    
    MockedStatic<ControlManagerCenter> controlManagerCenterMockedStatic;
    
    @Mock
    private ControlManagerCenter controlManagerCenter;
    
    @Mock
    private TpsControlManager tpsControlManager;
    
    @BeforeEach
    void setUp() {
        controlManagerCenterMockedStatic = Mockito.mockStatic(ControlManagerCenter.class);
        controlManagerCenterMockedStatic.when(() -> ControlManagerCenter.getInstance())
                .thenReturn(controlManagerCenter);
        when(controlManagerCenter.getTpsControlManager()).thenReturn(tpsControlManager);
        
        Map<String, Object> handlerMap = new HashMap<>();
        handlerMap.put(HealthCheckRequestHandler.class.getSimpleName(), new HealthCheckRequestHandler());
        Mockito.when(applicationContext.getBeansOfType(Mockito.any())).thenReturn(handlerMap);
        
        registry.onApplicationEvent(contextRefreshedEvent);
        
    }
    
    @AfterEach
    public void after() {
        controlManagerCenterMockedStatic.close();
    }
    
    @Test
    void testGetByRequestType() {
        assertNotNull(registry.getByRequestType(HealthCheckRequest.class.getSimpleName()));
    }
    
    @Test
    public void testSourceInvokeAllowed() {
        Map<String, Object> handlerMap = new HashMap<>();
        handlerMap.put(ServerReloadRequest.class.getSimpleName(), new ServerReloaderRequestHandler());
        Mockito.when(applicationContext.getBeansOfType(Mockito.any())).thenReturn(handlerMap);
        
        registry.onApplicationEvent(contextRefreshedEvent);
        assertNotNull(registry.sourceRegistry.get(ServerReloadRequest.class.getSimpleName())
                .contains(RemoteConstants.LABEL_SOURCE_CLUSTER));
        
        assertFalse(registry.checkSourceInvokeAllowed(ServerReloadRequest.class.getSimpleName(),
                RemoteConstants.LABEL_SOURCE_SDK));
        
    }
}
