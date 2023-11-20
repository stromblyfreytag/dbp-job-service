package com.trustwave.dbpjobservice.workflow;

public class Holder<T>
{
	T object = null;

	public T get() {
		return object;
	}

	public void set(T object) {
		this.object = object;
	}
}
