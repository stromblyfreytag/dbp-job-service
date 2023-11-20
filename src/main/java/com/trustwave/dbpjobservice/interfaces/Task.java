package com.trustwave.dbpjobservice.interfaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Task 
{
	private String name;
	private String category;
	private boolean essential;
	private List<String> actionNames =  // 'linearized' list of actions constituting task.
			new ArrayList<String>();    // Includes only 'mainstream' nodes (reachable through
			                            // default arcs)
	private List<Integer> expectedTimes = new ArrayList<>();

	public Task() 
	{
	}
	
	public Task(String name, String category, boolean essential) 
	{
		this.name = name;
		this.category = category;
        this.essential = essential;
	}
	
	public Task(Task other) 
	{
		this.name = other.name;
		this.category = other.category;
		this.essential = other.essential;
		this.actionNames.addAll( other.actionNames );
		this.expectedTimes.addAll( other.expectedTimes );
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}

    public void setEssential(boolean essential) {
        this.essential = essential;
    }
    public boolean isEssential() {
        return essential;
    }

	public List<String> getActionNames() {
		return actionNames;
	}
	
	public Task addAction( String actionName, int expectedTime )
	{
		actionNames.add( actionName != null? actionName: "" );
		expectedTimes.add( expectedTime > 0? expectedTime: 0 );
		return this;
	}

	public int getExpectedTime( String actionName )
	{
		for (int i = 0;  i < actionNames.size();  i++) {
			if (actionNames.get(i).equals( actionName )) {
				return (i < expectedTimes.size()? expectedTimes.get( i ): 0);
			}
		}
		// not found
		return 0;
	}
	
	public boolean isNoTimeAction( String actionName ) {
		return getExpectedTime( actionName ) == 0;
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public int hashCode() 
	{
		return name == null? 0: name.hashCode();
	}
	
	@Override
	public String toString() 
	{
		return name + (essential? ",essential": "") + ", actions=" + actionNames;
	}
	
}
