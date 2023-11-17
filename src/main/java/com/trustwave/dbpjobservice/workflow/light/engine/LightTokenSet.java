package com.trustwave.dbpjobservice.workflow.light.engine;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Set;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.Engine;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.env.TokenSetMemberEnv;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.db.LightDaoHolder;
import com.trustwave.dbpjobservice.workflow.light.db.LightTokenSetRecord;

public class LightTokenSet implements TokenSet {
    protected SoftReference<Set<ArcToken>> activeArcTokensRef = new SoftReference<>(null);
    protected SoftReference<Set<NodeToken>> activeNodeTokensRef = new SoftReference<>(null);
    protected Env env = null;
    private LightTokenSetRecord record;
    private InitialEnvironmentHandler initialEnvHandler =
            new InitialEnvironmentHandler();

    public LightTokenSet(LightTokenSetRecord record) {
        this.record = record;
    }

    @Override
    public String getName() {
        return record.getId().getName();
    }

    @Override
    public LightGraphProcess getProcess() {
        return getDao().findProcess(record.getId().getProcessId());
    }

    @Override
    public synchronized Set<ArcToken> getActiveArcTokens(final Engine engine) {
        Set<ArcToken> activeArcTokens = activeArcTokensRef.get();
        if (activeArcTokens == null) {
            activeArcTokens = getProcess().getActiveTokenSetArcTokens(getName());
            activeArcTokensRef = new SoftReference<>(activeArcTokens);
        }
        return activeArcTokens;
    }

    @Override
    public synchronized Set<NodeToken> getActiveNodeTokens(final Engine engine) {
        Set<NodeToken> activeNodeTokens = activeNodeTokensRef.get();
        if (activeNodeTokens == null) {
            activeNodeTokens = getProcess().getActiveTokenSetNodeTokens(getName());
            activeNodeTokensRef = new SoftReference<>(activeNodeTokens);
        }
        // ensure non-tokenset tokens are not included:
        Iterator<NodeToken> it = activeNodeTokens.iterator();
        while (it.hasNext()) {
            LightNodeToken token = (LightNodeToken) it.next();
            if (token.getTokenSetName() == null) {
                it.remove();
            }
        }
        return activeNodeTokens;
    }

    @Override
    public int getMaxMemberIndex() {
        return record.getSize();
    }

    // TODO: not used by Sarasvati any more
    public boolean isComplete() {
        return record.isComplete();
    }

    // TODO: not used by Sarasvati any more
    public void markComplete(final Engine engine) {
        record.setComplete(true);
    }

    // TODO: not used by Sarasvati any more
    public void reactivateForBacktrack(final Engine engine) {
        record.setComplete(false);
    }

    @Override
    public Env getEnv() {
        return record.getEnv();
    }

    public TokenSetMemberEnv getMemberEnv() {
        return new LightTokenSetMemberEnv(this);
    }

    @Override
    public int getLevel() {
        // TODO Auto-generated method stub
        return 1;
    }

    //@Override
    public String getStartingNodeName() {
        // TODO Auto-generated method stub
        return null;
    }

    public InitialEnvironmentHandler getInitialEnv() {
        return initialEnvHandler;
    }

    public Env removeInitialMemberEnv(int memberIndex) {
        if (!initialEnvHandler.hasMemberEnv(memberIndex)) {
            return null;
        }
        return initialEnvHandler.removeMemberEnv(memberIndex);
    }

    public LightTokenSetRecord getRecord() {
        return record;
    }

    public void setRecord(LightTokenSetRecord record) {
        this.record = record;
    }

    protected LightDao getDao() {
        return LightDaoHolder.getInstance();
    }

    @Override
    public String toString() {
        return "LightTokenSet[" + getName() + ", pid=" + record.getId().getProcessId() + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((record == null) ? 0 : record.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
        LightTokenSet other = (LightTokenSet) obj;
        if (record == null) {
			if (other.record != null) {
				return false;
			}
        }
        else if (!record.equals(other.record)) {
			return false;
		}
        return true;
    }

}