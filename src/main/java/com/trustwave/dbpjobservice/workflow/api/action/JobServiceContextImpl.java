package com.trustwave.dbpjobservice.workflow.api.action;

import javax.xml.ws.Service;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;



public class JobServiceContextImpl implements JobServiceContext, BeanFactoryAware {
    private static Logger logger = LogManager.getLogger(JobServiceContextImpl.class);

    private static JobServiceContextImpl instance;

    private static long serviceCount = 0;
    private BeanFactory beanFactory;
//    private ServiceFactory basicServiceFactory;
//    private ServiceFactory ldapServiceFactory;
//    private ServiceFactory secureLdapServiceFactory;
    private ThreadLocal<Map<String, Object>> endpointMaps =
            new ThreadLocal<Map<String, Object>>() {
                @Override
                public Map<String, Object> initialValue() {
                    return new HashMap<String, Object>();
                }
            };

    public static JobServiceContext createInstance() {
        instance = new JobServiceContextImpl();
        return instance;
    }

    public static JobServiceContextImpl getInstance() {
        return instance;
    }

    private synchronized long incrCount() {
        return ++serviceCount;
    }

    private synchronized long decrCount() {
        return --serviceCount;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        return (T) beanFactory.getBean(name, clazz);
    }

    @Override
    public <T> T getServiceEndpoint(Class<T> endpointInterface, String nameInOpenDs) {
//        Map<String, Object> map = endpointMaps.get();
//        @SuppressWarnings("unchecked")
//        T endpoint = (T) map.get(nameInOpenDs);
//        if (endpoint == null) {
//            endpoint = ldapServiceFactory.tryGetPort(endpointInterface, nameInOpenDs);
//            incrCount();
//            map.put(nameInOpenDs, endpoint);
//        }
//        return endpoint;
        throw new NotImplementedException("not yet implemented");
    }

    @Override
    public <T> T getSecureServiceEndpoint(final Class<T> endpointInterface, final String nameInOpenDs) {
//        Map<String, Object> map = endpointMaps.get();
//        @SuppressWarnings("unchecked")
//        T endpoint = (T) map.get(nameInOpenDs);
//        if (endpoint == null) {
//            endpoint = secureLdapServiceFactory.tryGetPort(endpointInterface, nameInOpenDs);
//            incrCount();
//            map.put(nameInOpenDs, endpoint);
//        }
//        return endpoint;
        throw new NotImplementedException("not yet implemented");
    }

    @Override
    public <T> T getServiceEndpointWithUrl(Class<? extends Service> serviceType,
            String endpointUrl) {
//        Map<String, Object> map = endpointMaps.get();
//        @SuppressWarnings("unchecked")
//        T endpoint = (T) map.get(endpointUrl);
//        if (endpoint == null) {
//            endpoint = basicServiceFactory.getPort(serviceType, endpointUrl);
//            incrCount();
//            map.put(endpointUrl, endpoint);
//        }
//        return endpoint;
        throw new NotImplementedException("not yet implemented");
    }

    public void closeAllEndpointsInCurrentThread() {
        Map<String, Object> map = endpointMaps.get();
        List<Object> endpoints = new ArrayList<Object>(map.values());
        map.clear();

        for (Object endpoint : endpoints) {
            closeEndpoint(endpoint);
        }
        endpoints.clear();
    }

    public void closeEndpointInCurrrentThread(String endpointName) {
        Map<String, Object> map = endpointMaps.get();
        Object endpoint = map.remove(endpointName);
        closeEndpoint(endpoint);
    }

    private void closeEndpoint(Object endpoint) {
//        if (endpoint == null) {
//            logger.warn("Attempt to close absent endpoint", new NullPointerException());
//            return;
//        }
//        if (endpoint instanceof Closeable) {
//            try {
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Closing " + Util.portString(endpoint) + ", count=" + serviceCount);
//                }
//                ((Closeable) endpoint).close();
//                decrCount();
//            }
//            catch (Exception e) {
//                logger.warn("Cannot close " + Util.portString(endpoint) + e.getMessage(), e);
//            }
//        }
    }

//    public void setBasicServiceFactory(ServiceFactory basicServiceFactory) {
//        this.basicServiceFactory = basicServiceFactory;
//    }
//
//    public void setLdapServiceFactory(ServiceFactory ldapServiceFactory) {
//        this.ldapServiceFactory = ldapServiceFactory;
//    }
//
//    public void setSecureLdapServiceFactory(ServiceFactory secureLdapServiceFactory) {
//        this.secureLdapServiceFactory = secureLdapServiceFactory;
//    }

}

