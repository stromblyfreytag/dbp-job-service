package com.trustwave.dbpjobservice.workflow.light.engine;

import com.googlecode.sarasvati.NodeTokenSetMember;
import com.googlecode.sarasvati.env.Env;

public class LightNodeTokenSetMember implements NodeTokenSetMember {
    protected LightTokenSet tokenSet;
    protected LightNodeToken token;
    protected int memberIndex;

    public LightNodeTokenSetMember(LightTokenSet tokenSet,
            LightNodeToken token,
            int memberIndex) {
        this.tokenSet = tokenSet;
        this.token = token;
        this.memberIndex = memberIndex;
    }

    @Override
    public LightTokenSet getTokenSet() {
        return tokenSet;
    }

    @Override
    public LightNodeToken getToken() {
        return token;
    }

    @Override
    public int getMemberIndex() {
        return memberIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + memberIndex;
        result = prime * result + ((token == null) ? 0 : token.hashCode());
        result = prime * result + ((tokenSet == null) ? 0 : tokenSet.hashCode());
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
        LightNodeTokenSetMember other = (LightNodeTokenSetMember) obj;
		if (memberIndex != other.memberIndex) {
			return false;
		}
        if (token == null) {
			if (other.token != null) {
				return false;
			}
        }
        else if (!token.equals(other.token)) {
			return false;
		}
        if (tokenSet == null) {
			if (other.tokenSet != null) {
				return false;
			}
        }
        else if (!tokenSet.equals(other.tokenSet)) {
			return false;
		}
        return true;
    }

    @Override
    public Env getEnv() {
        return token.getEnv();
    }
}