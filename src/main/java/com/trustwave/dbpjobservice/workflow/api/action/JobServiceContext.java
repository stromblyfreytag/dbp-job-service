package com.trustwave.dbpjobservice.workflow.api.action;

import javax.xml.ws.Service;

public interface JobServiceContext {
    /**
     * Retrieve a Spring bean from JobService Spring context
     *
     * @param name bean name
     * @param clazz bean class or interface
     * @return Spring bean object from JobService context or <code>null</code>
     * if bean with the specified name was not found.
     */
    public <T> T getBean(String name, Class<T> clazz);

    /**
     * Get service endpoint. Endpoint url is auto-configured from the
     * corresponding OpenDs service entry.
     *
     * @param endpointInterface Service endpoint interface class
     * @param serviceNameInOpenDs Name of the service entry in OpenDs.
     * @return configured service endpoint.
     */
    public <T> T getServiceEndpoint(Class<T> endpointInterface,
            String serviceNameInOpenDs);

    /**
     * Get STS Secured service endpoint. Endpoint url is auto-configured from the
     * corresponding OpenDs service entry.
     *
     * @param endpointInterface Service endpoint interface class
     * @param serviceNameInOpenDs Name of the service entry in OpenDs.
     * @return configured service endpoint.
     */
    public <T> T getSecureServiceEndpoint(Class<T> endpointInterface,
            String serviceNameInOpenDs);

    /**
     * Uses the Service object to create a port with the endpoint
     * address configured. The return type will be the type of the port that
     * the Service specifies.
     *
     * @param <T> the port type.
     * @param serviceType a class that extends Service and has at least one port
     * defined.
     * @param endpointUrl the URL of the destination service.
     * @return a configured web service port. The port will be of the type
     * specified in the Service parameter.
     */
    <T> T getServiceEndpointWithUrl(Class<? extends Service> serviceType, String endpointUrl);
}
