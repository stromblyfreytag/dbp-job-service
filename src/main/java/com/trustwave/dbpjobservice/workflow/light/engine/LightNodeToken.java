package com.trustwave.dbpjobservice.workflow.light.engine;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.GuardAction;
import com.googlecode.sarasvati.Node;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.NodeTokenSetMember;
import com.googlecode.sarasvati.TokenSet;
import com.googlecode.sarasvati.env.Env;
import com.googlecode.sarasvati.impl.NestedEnv;
import com.googlecode.sarasvati.util.SvUtil;
import com.googlecode.sarasvati.visitor.TokenVisitor;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.db.LightDaoHolder;
import com.trustwave.dbpjobservice.workflow.light.db.LightTokenRecord;
import com.trustwave.dbpjobservice.workflow.light.db.LightUtils;
import com.trustwave.dbpjobservice.workflow.light.engine.LightArcToken.ArcTokenState;

public class LightNodeToken implements NodeToken {
    private LightTokenRecord record;
    private SoftReference<List<ArcToken>> parentTokensRef = null;
    private Set<NodeTokenSetMember> tokenSetMemberships = null;
    private List<ArcToken> childTokens = null;

    public LightNodeToken(LightTokenRecord record) {
        this.record = record;
    }

    @Override
    public Long getId() {
        return record.getTokenId();
    }

    @Override
    public Node getNode() {
        return getDao().findNode(record.getNodeId());
    }

    @Override
    public LightGraphProcess getProcess() {
        return getDao().findProcess(record.getProcessId());
    }

    @Override
    public GuardAction getGuardAction() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGuardAction(final GuardAction action) {
    }

    @Override
    public List<ArcToken> getParentTokens() {
        List<ArcToken> parentTokens =
                parentTokensRef != null ? parentTokensRef.get() : null;
        if (parentTokens == null) {
            Collection<String> parentArcTokenIds = record.getParentArcs();
            parentTokens = new ArrayList<>(parentArcTokenIds.size());
            for (String arcTokenId : parentArcTokenIds) {
                parentTokens.add(
                        new LightArcToken(arcTokenId, ArcTokenState.Complete));
            }
            parentTokensRef = new SoftReference<>(parentTokens);
        }
        return parentTokens;
    }

    @Override
    public List<ArcToken> getChildTokens() {
        if (childTokens == null) {
            childTokens = new ArrayList<>(1);
        }
        return childTokens;
    }

    @Override
    public Date getCreateDate() {
        return record.getCreateDate();
    }

    @Override
    public boolean isComplete() {
        return record.getCompleteDate() != null;
    }

    @Override
    public void markComplete() {
        record.setCompleteDate(new Date());
    }

    @Override
    public Date getCompleteDate() {
        return record.getCompleteDate();
    }

    @Override
    public void accept(final TokenVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Env getFullEnv() {
        return new NestedEnv(getEnv(), getProcess().getEnv());
    }

    @Override
    public Env getEnv() {
        return record.getEnv();
    }

    public String getEnvString() {
        return record.getEnvString();
    }

    @Override
    public ExecutionType getExecutionType() {
        return ExecutionType.valueOf(record.getExecutionType());
    }

    @Override
    public void markBacktracked() {
        ExecutionType newextype =
                getExecutionType().getCorrespondingBacktracked(isComplete());
        record.setExecutionType(newextype.name());
    }

    @Override
    public LightTokenSet getTokenSet(String name) {
        if (name == null || !name.equals(record.getTokenSetName())) {
            return null;
        }
        return getDao().findTokenSet(record.getProcessId(), name);
    }

    public String getTokenSetName() {
        return record.getTokenSetName();
    }

    public Integer getMemberIndex() {
        return record.getThreadId();
    }

    @Override
    public NodeTokenSetMember getTokenSetMember(String name) {
        return (NodeTokenSetMember) SvUtil.getTokenSetMember(this, name);
    }

    @Override
    public Set<NodeTokenSetMember> getTokenSetMemberships() {
        if (tokenSetMemberships == null) {
            tokenSetMemberships = calculateTokensetMembership();
        }
        // ensure membership is empty if token was not added to a token set
        if (getTokenSetName() == null) {
            tokenSetMemberships.clear();
        }
        return tokenSetMemberships;
    }

    private Set<NodeTokenSetMember> calculateTokensetMembership() {
        Set<NodeTokenSetMember> set = new HashSet<>(1);

        if (record.getTokenSetName() != null) {
            LightTokenSet tokenSet = getTokenSet(record.getTokenSetName());
            NodeTokenSetMember member =
                    new LightNodeTokenSetMember(tokenSet, this, record.getThreadId());
            set.add(member);
        }
        return set;
    }

    public void addToTokenset(TokenSet tokenSet, int memberIndex) {
        if (LightUtils.isTokenSetMergeNode(getNode(), tokenSet.getName())) {
            // this node merges that token set - should not be added to it!
            return;
        }
        if (record.getTokenSetName() != null) {
            if (record.getTokenSetName().equals(tokenSet.getName())
                    && getMemberIndex() != null && getMemberIndex().intValue() == memberIndex) {
                // added twice to the same token set, with the same id.
                // Strange, but we can live with that
            }
            else {
                throw new RuntimeException("addToTokenset: " + this
                        + ", ts='" + tokenSet.getName() + "', ind=" + memberIndex
                        + ": Already in token set '" + record.getTokenSetName()
                        + "', ind=" + getMemberIndex());
            }
        }
        record.setTokenSetName(tokenSet.getName());
        record.setThreadId(memberIndex);
    }

    @Override
    public String toString() {
        return "LightNodeToken[id=" + getId() + " node="
                + (getNode() == null ? null : getNode().getName())
                + " execType=" + getExecutionType()
                + (record.getTokenSetName() != null ?
                (" ts=" + record.getTokenSetName() + ", " + getMemberIndex()) : "")
                + "]";
    }

    public LightTokenRecord getRecord() {
        return record;
    }

    public void setRecord(LightTokenRecord rec) {
        if (rec == null || record.getTokenId() != rec.getTokenId()) {
            throw new RuntimeException("Invalid record id="
                    + (rec != null ? rec.getTokenId() : null)
                    + ", " + this);
        }
        this.record = rec;
    }

    private LightDao getDao() {
        return LightDaoHolder.getInstance();
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
        LightNodeToken other = (LightNodeToken) obj;
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

    @Override
    public Date getDelayUntilTime() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getDelayCount() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void markDelayed(Date delayUntilTime) {
        // TODO Auto-generated method stub

    }
}