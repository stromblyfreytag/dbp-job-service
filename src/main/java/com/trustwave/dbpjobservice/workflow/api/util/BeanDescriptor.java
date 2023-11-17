package com.trustwave.dbpjobservice.workflow.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanDescriptor
{
    private Class<?> targetClazz;
    private boolean caseInsensitive;
    private boolean stickyCollections;
    private Map<String, List<PropertyHandler>> handlers =
            new HashMap<String, List<PropertyHandler>>();
    private Map<String,PropertyHandlerFactory> customFactories =
            new HashMap<String, PropertyHandlerFactory>();

    public BeanDescriptor( Class<?> targetClazz )
    {
        this( targetClazz, false, false );
    }

    public BeanDescriptor(Class<?> targetClazz, boolean caseInsensitive) {
        this( targetClazz, caseInsensitive, false );
    }

    /**
     * @param targetClazz target class of the descriptor
     * @param caseInsensitive if property names should be considered case-insensitive
     * @param stickyCollections if collections are 'sticky', i.e. if property is a collection,
     *                          subproperties will be returned as collections (lists) as well.
     *                          WARNING: setting properties does not work with sticky collections yet.
     */
    public BeanDescriptor(Class<?> targetClazz, boolean caseInsensitive, boolean stickyCollections)
    {
        this.targetClazz = targetClazz;
        this.caseInsensitive = caseInsensitive;
        this.stickyCollections = stickyCollections;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public synchronized void setCaseInsensitive(boolean caseInsensitive)
    {
        this.caseInsensitive = caseInsensitive;
        handlers.clear();
    }

    public boolean isUseIndexForCollections() {
        return stickyCollections;
    }

    public synchronized void setPropertyHandlerFactory( String propertyName,
            PropertyHandlerFactory factory )
    {
        customFactories.put( propertyName, factory );
        customFactories.put( propertyName.toLowerCase(), factory );
        handlers.clear();
    }

    /**
     * Check if bean has declared property with the specified name
     * @param propertyName property name
     * @return <code>true</code> if bean has declared property with the specified
     *         name, <code>false</code> otherwise.
     */
    public boolean hasDeclaredProperty( String propertyName )
    {
        try {
            getPropertyHandlers( propertyName );
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Get list of property handlers by property name.
     * List contains one property handler for simple property name,
     * or several for dotted property name.
     * @param name property name, e.g. 'type.genericType.name'
     * @return list of property handlers, one handler per simple name.
     * @throws IllegalArgumentException if bean class does not have
     *         public property with the specified name.
     */
    public synchronized List<PropertyHandler> getPropertyHandlers( String name )
    {
        List<PropertyHandler> phlist = handlers.get( name );
        if (phlist == null) {
            PropertyHandlerFactory factory = new PropertyHandlerFactory();
            factory.setCaseInsensitive( caseInsensitive );
            phlist = new ArrayList<PropertyHandler>();
            findPropertyHandlers( "", name, targetClazz, phlist, factory );
            handlers.put( name, phlist );
        }
        // Descriptor lists are cached for performance, but clients can modify
        // returned list (and do, see setProperty() method of Bean class).
        // So, return a copy of cached list to not depend on client's good will
        // in maintaining cache integrity ...
        return new ArrayList<PropertyHandler>( phlist );
    }

    protected void findPropertyHandlers(
            String initialPath,
            String name, Class<?> beanClass,
            List<PropertyHandler> phlist,
            PropertyHandlerFactory factory )
    {
        int k = name.indexOf( '.' );
        String propertyName = (k>=0? name.substring( 0, k ): name);
        factory = getPhFactory( propertyName, factory );
        String propertyPath = initialPath + propertyName;
        List<PropertyHandler> saved = handlers.get( propertyPath );
        if (saved == null) {
            factory.addPropertyHandler( beanClass, propertyName, phlist, stickyCollections );
            handlers.put( propertyPath, new ArrayList<PropertyHandler>( phlist ) );
        }
        else {
            phlist.clear();
            phlist.addAll( saved );
        }

        if (k > 0) {
            name = name.substring( k+1 );
            beanClass = phlist.get( phlist.size()-1 ).getElementClass();
            findPropertyHandlers( propertyPath + ".", name, beanClass, phlist, factory );
        }
    }

    /** Get reference to the {@link IndexPropertyHandler} for the specified collection property -
     * to control index of retrieved/modified collection elements.
     *
     * @param propertyName
     *
     * @see Bean
     * @see IndexPropertyHandler
     */
    public IndexPropertyHandler getCollectionIndex( String propertyName )
    {
        List<PropertyHandler> handlers = getPropertyHandlers( propertyName );
        PropertyHandler handler = handlers.get( handlers.size() - 1 );
        if (!(handler instanceof IndexPropertyHandler)) {
            throw new RuntimeException( "Not a collection property: " + propertyName );
        }
        return (IndexPropertyHandler)handler;
    }

    private PropertyHandlerFactory getPhFactory( String propertyName, PropertyHandlerFactory currentFactory )
    {
        if (caseInsensitive) {
            propertyName  = propertyName.toLowerCase();
        }
        PropertyHandlerFactory factory = customFactories.get( propertyName );
        if (factory == null) {
            factory = currentFactory;
        }
        else {
            factory.setCaseInsensitive( currentFactory.isCaseInsensitive() );
        }
        return factory;
    }

    public Class<?> getPropertyType( String propertyName )
    {
        List<PropertyHandler> handlers = getPropertyHandlers( propertyName  );
        return handlers.get( handlers.size()-1 ).getElementClass();
    }
}
