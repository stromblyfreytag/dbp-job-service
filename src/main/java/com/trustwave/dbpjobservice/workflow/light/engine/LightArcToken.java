package com.trustwave.dbpjobservice.workflow.light.engine;

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.googlecode.sarasvati.ArcToken;
import com.googlecode.sarasvati.ArcTokenSetMember;
import com.googlecode.sarasvati.ExecutionType;
import com.googlecode.sarasvati.NodeToken;
import com.googlecode.sarasvati.hib.HibArc;
import com.googlecode.sarasvati.visitor.TokenVisitor;
import com.trustwave.dbpjobservice.impl.Messages;
import com.trustwave.dbpjobservice.workflow.api.util.EscapingEncoder;
import com.trustwave.dbpjobservice.workflow.light.db.LightDao;
import com.trustwave.dbpjobservice.workflow.light.db.LightDaoHolder;
import com.trustwave.dbpjobservice.workflow.light.db.LightUtils;

public class LightArcToken implements ArcToken {
    private static String NoTokenSet = new String(new char[]{0});
    protected Set<ArcTokenSetMember> tokenSetMemberships = null;
    private String id = null;
    private Long parentTokenId = null;
    private HibArc arc;
    private String tokenSetName;
    private Integer memberIndex;
    private String initialEnvString;
    private SoftReference<LightNodeToken> parentTokenRef;
    private ArcTokenState state;

    public LightArcToken(String id, ArcTokenState state) {
        this.id = id;
        this.state = state;
    }

    public LightArcToken(LightNodeToken parentToken,
            HibArc arc,
            ArcTokenState state,
            String initialEnvString) {
        this.parentTokenRef = new SoftReference<>(parentToken);
        this.parentTokenId = parentToken.getId();
        this.arc = arc;
        this.state = state;
    }

    private static LightDao getDao() {
        return LightDaoHolder.getInstance();
    }

    @Override
    public HibArc getArc() {
        if (parentTokenId == null) {
            deserialize();
        }
        return arc;
    }

    @Override
    public LightGraphProcess getProcess() {
        return getParentToken().getProcess();
    }

    @Override
    public LightNodeToken getParentToken() {
        LightNodeToken parentToken =
                (parentTokenRef != null ? parentTokenRef.get() : null);
        if (parentToken == null) {
            parentToken = getDao().findNodeToken(getParentTokenId());
            parentTokenRef = new SoftReference<>(parentToken);
        }
        return parentToken;
    }

    public long getParentTokenId() {
        if (parentTokenId == null) {
            deserialize();
        }
        return parentTokenId.longValue();
    }

    public String getInitialEnvString() {
        if (parentTokenId == null) {
            deserialize();
        }
        return initialEnvString;
    }

    void setInitialEnvString(String initialEnvString) {
        if (id != null) {
            throw new RuntimeException(Messages.getString("workflow.intialEnv.canNotChange", this));
        }
        this.initialEnvString = initialEnvString;
    }

    public String getEnvString() {
        return LightUtils.mergeEnvStrings(
                getInitialEnvString(),
                getParentToken().getEnvString());
    }

    @Override
    public NodeToken getChildToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isComplete() {
        return state == ArcTokenState.Complete;
    }

    @Override
    public void markComplete(final NodeToken token) {
        state = ArcTokenState.Complete;
    }

    @Override
    public boolean isPending() {
        return state == ArcTokenState.Pending;
    }

    @Override
    public void markProcessed() {
        state = ArcTokenState.Active;
    }

    @Override
    public ExecutionType getExecutionType() {
        return ExecutionType.Forward;
    }

    @Override
    public void markBacktracked() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(final TokenVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public Set<ArcTokenSetMember> getTokenSetMemberships() {
        if (tokenSetMemberships == null) {
            tokenSetMemberships = calculateTokensetMembership();
        }
        return tokenSetMemberships;
    }

    private Set<ArcTokenSetMember> calculateTokensetMembership() {
        if (parentTokenId == null) {
            deserialize();
        }
        Set<ArcTokenSetMember> set = new HashSet<>(1);
        String tsName = getTokenSetName();
        if (tsName != null) {
            LightTokenSet ts =
                    getDao().findTokenSet(getProcess().getId(), tsName);
            ArcTokenSetMember member =
                    new LightArcTokenSetMember(ts, this, getMemberIndex());
            set.add(member);
        }
        return set;
    }

    public String getTokenSetName() {
        if (parentTokenId == null) {
            deserialize();
        }
        if (tokenSetName == null) {
            tokenSetName = getParentToken().getTokenSetName();
            if (tokenSetName == null) {
                tokenSetName = NoTokenSet;
            }
        }
        // == (rather than equals) is correct
        return tokenSetName == NoTokenSet ? null : tokenSetName;
    }

    public int getMemberIndex() {
        if (parentTokenId == null) {
            deserialize();
        }
        if (memberIndex == null) {
            memberIndex = getParentToken().getMemberIndex();
            if (memberIndex == null) {
                memberIndex = -1;
            }
        }
        return memberIndex.intValue();
    }

    boolean isFirstArcInTokenSet() {
        String tsName = getTokenSetName();
        return tsName != null && !tsName.equals(getParentToken().getTokenSetName());
    }

    public String getIdString() {
        if (id == null) {
            id = serialize(true);
        }
        return id;
    }

    private EscapingEncoder createEncoder() {
        return new EscapingEncoder('+', '`');
    }

    String serialize(boolean withEnv) {
        StringBuilder sb = new StringBuilder();
        EscapingEncoder encoder = createEncoder();
        encoder.encode(Long.toHexString(getParentTokenId()), sb);
        encoder.encode(Long.toHexString(getArc().getId()), sb);
        if (getMemberIndex() != -1) {
            encoder.encode(Integer.toHexString(getMemberIndex()), sb);
            if (isFirstArcInTokenSet() && withEnv) {
                encoder.encode(getTokenSetName(), sb);
                encoder.encode(initialEnvString, sb);
            }
        }
        encoder.finishEncode(sb);
        return sb.toString();
    }

    private void deserialize() {
        List<String> elems = createEncoder().decode(id);

        this.parentTokenId = Long.valueOf(elems.get(0), 16);
        long arcId = Long.valueOf(elems.get(1), 16);
        this.arc = getDao().findArc(arcId);

        if (elems.size() > 2) {
            this.memberIndex = Integer.valueOf(elems.get(2), 16);
            if (elems.size() > 3) {
                this.tokenSetName = elems.get(3);
                this.initialEnvString = elems.get(4);
            }
        }
    }

    public String getShortId() {
        return serialize(false);
    }

    @Override
    public String toString() {
        return "LightArcToken[" + (id != null ? id : getShortId())
                + (arc != null ? ", arc=" + getArc().getName() : null)
                + ", " + state.name() + "]";
    }

    @Override
    public int hashCode() {
        if (parentTokenId == null) {
            deserialize();
        }
        final int prime = 31;
        int result = 1;
        result = prime * result + arc.getId().hashCode();
        result = prime * result + getMemberIndex();
        result = prime * result + parentTokenId.hashCode();
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
        LightArcToken other = (LightArcToken) obj;
        if (parentTokenId == null) {
            deserialize();
        }
        if (other.parentTokenId == null) {
            other.deserialize();
        }

		if (!parentTokenId.equals(other.parentTokenId)) {
			return false;
		}
		if (!arc.getId().equals(other.arc.getId())) {
			return false;
		}
		if (getMemberIndex() != other.memberIndex) {
			return false;
		}
        return true;
    }

    @Override
    public Long getId() {
        // This call is used only in TokenSetDeadEndListener,
        // which is installed only by HibEngine; not used in light engine
        return null;
    }

    @Override
    public boolean isTokenSetMember() {
        return !getTokenSetMemberships().isEmpty();
    }

    void setTokenSetMember(LightArcTokenSetMember tsMember) {
        this.tokenSetName = tsMember.getTokenSet().getName();
        this.memberIndex = tsMember.getMemberIndex();
    }

    public static enum ArcTokenState {
        Pending,
        Active,
        Complete,
    }

}