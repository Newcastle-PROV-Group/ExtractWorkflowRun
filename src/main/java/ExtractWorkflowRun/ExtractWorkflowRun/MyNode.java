package ExtractWorkflowRun.ExtractWorkflowRun;

import java.util.Date;

import org.neo4j.graphdb.Node;

public class MyNode implements Comparable<MyNode>  {

	private Node node;
	private Date date;
	public Node getNode() {
		return node;
	}
	public void setNode(Node node) {
		this.node = node;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}

	public int compareTo(MyNode arg0) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		return this.getDate().compareTo(arg0.getDate());
	}
	
	
	
}
