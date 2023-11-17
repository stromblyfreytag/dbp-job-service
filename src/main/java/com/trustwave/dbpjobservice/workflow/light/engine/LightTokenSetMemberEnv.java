package com.trustwave.dbpjobservice.workflow.light.engine;

import java.util.List;

import com.googlecode.sarasvati.env.TokenSetMemberEnv;

public class LightTokenSetMemberEnv implements TokenSetMemberEnv {
    private LightTokenSet tokenSet;

    public LightTokenSetMemberEnv(LightTokenSet tokenSet) {
        this.tokenSet = tokenSet;
    }

    @Override
    public String getAttribute(int memberIndex, String name) {
        return tokenSet.getInitialEnv().getAttribute(memberIndex, name);
    }

    @Override
    public <T> T getAttribute(int memberIndex, String name, Class<T> type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getAttributeNames(int memberIndex) {
        return tokenSet.getInitialEnv().getAttributeNames();
    }

    @Override
    public boolean hasAttribute(int memberIndex, String name) {
        return tokenSet.getInitialEnv().getAttributeNames().contains(name);
    }

    @Override
    public void setAttribute(int memberIndex, String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(int memberIndex, String name, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttribute(String name, List<?> values) {
        tokenSet.getInitialEnv().setAttribute(name, values);
    }

    @Override
    public void removeAttribute(int memberIndex, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getTransientAttribute(int memberIndex, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<String> getTransientAttributeNames(int memberIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTransientAttribute(int memberIndex, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientAttribute(int memberIndex, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransientAttribute(int memberIndex, String name, Object value) {
        throw new UnsupportedOperationException();
    }

}