package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class Event 
{
	/** event name (type) */
	private String name;
	
	/** target process ID, if null - any waiting process */
	private Long   processId;
	
	/** target task item (e.g. asset), if null - any waiting task */
	private String item;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getProcessId() {
		return processId;
	}
	public void setProcessId(Long processId) {
		this.processId = processId;
	}
	public String getItem() {
		return item;
	}
	public void setItem(String item) {
		this.item = item;
	}
	
}
