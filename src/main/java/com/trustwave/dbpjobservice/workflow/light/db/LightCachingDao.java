package com.trustwave.dbpjobservice.workflow.light.db;

import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.hib.HibArc;
import com.googlecode.sarasvati.hib.HibGraph;
import com.googlecode.sarasvati.hib.HibNodeRef;
import com.trustwave.dbpjobservice.workflow.api.util.BooleanStatisticsCollector;
import com.trustwave.dbpjobservice.workflow.api.util.LruCache;
import com.trustwave.dbpjobservice.workflow.api.util.StatisticsCollector;
import com.trustwave.dbpjobservice.workflow.light.engine.LightGraphProcess;
import com.trustwave.dbpjobservice.workflow.light.engine.LightNodeToken;
import com.trustwave.dbpjobservice.workflow.light.engine.LightTokenSet;

public class LightCachingDao implements LightDao {
    private final static Logger logger = LogManager.getLogger(LightCachingDao.class);

    private LightHibernateDao dao;
    private int processCacheSize = 10;
    private int tokenSetCacheSize = -1;
    private int tokenCacheSize = 1000;
    private LruCache<Long, LightGraphProcess> processCache = null;
    private SoftReference<LruCache<Long, LightNodeToken>> tokenCacheRef = null;
    private LruCache<LightTokenSetId, LightTokenSet> tokenSetCache = null;
    private BooleanStatisticsCollector processCollector =
            StatisticsCollector.getInstance().createBooleanCollector("findProcess");
    private BooleanStatisticsCollector tokenCollector =
            StatisticsCollector.getInstance().createBooleanCollector("findToken");
    private BooleanStatisticsCollector tsCollector =
            StatisticsCollector.getInstance().createBooleanCollector("findTokenSet");

    public LightCachingDao() {
    }

    public void init() {
        dao.setCachingDao(this);
        if (tokenSetCacheSize < 0) {
            tokenSetCacheSize = processCacheSize * 2;
        }
        processCache = new LruCache<>(processCacheSize);
        tokenSetCache = new LruCache<>(tokenSetCacheSize);
        tokenCacheRef = new SoftReference<>(null);
        logger.info("Cache sizes: process: " + processCacheSize
                + ", tokenSet: " + tokenSetCacheSize
                + ", token: " + tokenCacheSize);
    }

    public HibGraph findGraph(Long graphId) {
        HibGraph graph = dao.findGraph(graphId);
        return graph;
    }

    public Node findNode(Long nodeId) {
        Node node = dao.findNode(nodeId);
        return node;
    }

    @Override
    public LightGraphProcess createProcess(HibGraph graph) {
        LightGraphProcess process = dao.createProcess(graph);
        if (processCacheSize > 0) {
            synchronized (processCache) {
                processCache.put(process.getId(), process);
            }
        }
        return process;
    }

    @Override
    public LightGraphProcess findProcess(Long processId) {
        if (processCacheSize <= 0) {
            return dao.findProcess(processId);
        }

        LightGraphProcess process = null;
        synchronized (processCache) {
            process = processCache.get(processId);
        }
        if (process != null) {
            processCollector.markSuccess();
            process.setRecord(dao.ensureAttached(process.getRecord()));
        }
        else {
            processCollector.markFailure();
            process = dao.findProcess(processId);
            synchronized (processCache) {
                processCache.put(process.getId(), process);
            }
        }
        return process;
    }

    @Override
    public void deleteProcess(Long processId) {
        if (processCacheSize > 0) {
            synchronized (processCache) {
                processCache.remove(processId);
            }
        }
        dao.deleteProcess(processId);
    }

    private synchronized LruCache<Long, LightNodeToken> getTokenCache() {
        LruCache<Long, LightNodeToken> tokenCache = tokenCacheRef.get();
        if (tokenCache == null) {
            tokenCache = new LruCache<Long, LightNodeToken>(tokenCacheSize);
            tokenCacheRef = new SoftReference<>(tokenCache);
        }
        return tokenCache;
    }

    @Override
    public LightNodeToken createNodeToken(LightGraphProcess process, HibNodeRef node,
            ExecutionType executionType, String envString, Collection<String> parentArcIds) {
        LightNodeToken token =
                dao.createNodeToken(process, node, executionType,
                        envString, parentArcIds);
        if (tokenCacheSize > 0) {
            LruCache<Long, LightNodeToken> tokenCache = getTokenCache();
            synchronized (tokenCache) {
                tokenCache.put(token.getId(), token);
            }
        }
        return token;
    }

    @Override
    public LightNodeToken getNodeToken(LightTokenRecord record) {
        if (tokenCacheSize <= 0) {
            return dao.getNodeToken(record);
        }

        LightNodeToken token = null;
        LruCache<Long, LightNodeToken> tokenCache = getTokenCache();
        synchronized (tokenCache) {
            token = tokenCache.get(record.getTokenId());
        }
        if (token != null) {
            token.setRecord(record);
        }
        else {
            token = dao.getNodeToken(record);
            synchronized (tokenCache) {
                tokenCache.put(record.getTokenId(), token);
            }
        }
        return token;
    }

    @Override
    public LightNodeToken findNodeToken(Long tokenId) {
        if (tokenCacheSize <= 0) {
            return dao.findNodeToken(tokenId);
        }

        LightNodeToken token = null;
        LruCache<Long, LightNodeToken> tokenCache = getTokenCache();
        synchronized (tokenCache) {
            token = tokenCache.get(tokenId);
        }

        if (token != null) {
            tokenCollector.markSuccess();
            token.setRecord(dao.ensureAttached(token.getRecord()));
        }
        else {
            tokenCollector.markFailure();
            token = dao.findNodeToken(tokenId);
            synchronized (tokenCache) {
                tokenCache.put(tokenId, token);
            }
        }
        return token;
    }

    @Override
    public List<NodeToken> findTokensOnNode(Long processId, Node node) {
        return dao.findTokensOnNode(processId, node);
    }

    @Override
    public List<NodeToken> findActiveTokens(Long processId) {
        return dao.findActiveTokens(processId);
    }

    @Override
    public HibArc findArc(Long arcId) {
        HibArc arc = dao.findArc(arcId);
        return arc;
    }

    @Override
    public LightTokenSet createTokenSet(LightGraphProcess process, String name,
            int maxMemberIndex) {
        LightTokenSet tokenSet =
                dao.createTokenSet(process, name, maxMemberIndex);
        if (tokenSetCacheSize > 0) {
            synchronized (tokenSetCache) {
                tokenSetCache.put(tokenSet.getRecord().getId(), tokenSet);
            }
        }
        return tokenSet;
    }

    @Override
    public LightTokenSet findTokenSet(Long processId, String name) {
        if (tokenSetCacheSize <= 0) {
            return dao.findTokenSet(processId, name);
        }

        LightTokenSetId id = new LightTokenSetId(processId, name);
        LightTokenSet tokenSet = null;
        synchronized (tokenSetCache) {
            tokenSet = tokenSetCache.get(id);
        }
        if (tokenSet != null) {
            tsCollector.markSuccess();
            tokenSet.setRecord(dao.ensureAttached(tokenSet.getRecord()));
        }
        else {
            tsCollector.markFailure();
            tokenSet = dao.findTokenSet(processId, name);
            synchronized (tokenSetCache) {
                tokenSetCache.put(id, tokenSet);
            }
        }
        return tokenSet;
    }

    @Override
    public Session getSession() {
        return dao.getSession();
    }

    public void setDao(LightHibernateDao dao) {
        this.dao = dao;
    }

    public void setProcessCacheSize(int processCacheSize) {
        this.processCacheSize = processCacheSize;
    }

    public void setTokenSetCacheSize(int tokensetCacheSize) {
        this.tokenSetCacheSize = tokensetCacheSize;
    }

    public void setTokenCacheSize(int tokenCacheSize) {
        this.tokenCacheSize = tokenCacheSize;
    }

}
