package ExtractWorkflowRun.ExtractWorkflowRun;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.IntervalCategoryDataset;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.kernel.impl.util.FileUtils;

import com.connexience.server.model.logging.provenance.model.nodes.DataVersion;
import com.connexience.server.model.logging.provenance.model.nodes.Library;
import com.connexience.server.model.logging.provenance.model.nodes.ServiceRun;
import com.connexience.server.model.logging.provenance.model.nodes.ServiceVersion;
import com.connexience.server.model.logging.provenance.model.nodes.TransientData;
import com.connexience.server.model.logging.provenance.model.nodes.User;
import com.connexience.server.model.logging.provenance.model.nodes.WorkflowRun;
import com.connexience.server.model.logging.provenance.model.nodes.WorkflowVersion;

public class CreateGraph {

	private GraphDatabaseService graphDb;
	private Transaction tx;
	private Transaction tx2;
	private Index<Node> escIdIndex;
	private GraphDatabaseService newgraphDb;
	private Map<String, Node> listNode = new HashMap<String, Node>();
	private List<String> rule = new ArrayList<String>();
	private List<Date> listDate = new ArrayList<Date>();
	private Date date;

	private enum RelTypes implements RelationshipType {
		// WAS_DERIVED_FROM, WAS_INFORMED_BY, USED, WAS_GENERATED_BY, HAD,
		// WITH_PLAN, WAS_ATTRIBUTED_TO;
		RUN_OF, USED, INSTANCE_OF, REQUIRED, CONTAINED, WAS_GENERATED_BY, VERSION_OF, INVOKED_BY;
	}

	public CreateGraph(GraphDatabaseService graphDb){
		this.graphDb=graphDb;
	}
	public void setUp(String nameFornewDB) {
		clearDb(nameFornewDB);
		newgraphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(nameFornewDB)
				.newGraphDatabase();

		registerShutdownHook();
		tx = graphDb.beginTx();
		tx2 = newgraphDb.beginTx();

	}

	public void deleteWorkflowRun() {
		Collection<Node> collection = listNode.values();
		Iterator<Node> it = collection.iterator();
		while (it.hasNext()) {
			Node node = it.next();
			if (node.getProperty("TYPE").equals("WorkflowRun")) {
				Iterable<Relationship> relationships = node.getRelationships();
				for (Relationship ship : relationships) {
					ship.delete();
				}
				node.delete();
			}
		}
	}

	private Date getDate(String date) {
		SimpleDateFormat format = new SimpleDateFormat(
				"EEE MMM dd HH:mm:ss 'UTC' yyyy", java.util.Locale.UK);
		// Wed Apr 30 09:31:36 UTC 2014
		Date d = null;
		try {
			d = format.parse(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return d;
	}

	private void generatServiceRunTime(String num) {
		try {
			File csv = new File(num+".csv"); // CSV文件
			// 追记模式
			csv.delete();
			csv.createNewFile();
			BufferedWriter bw;

			bw = new BufferedWriter(new FileWriter(csv, true));
			// 新增一行数据
			bw.write("name" + "," + "startTime" + "," + "endTime"
					+","+"blockUUID"+ "," + "invocationId"+","+"ServiceVersion");
			for (Node node : listNode.values()) {
				if (node.getProperty("TYPE").equals("ServiceRun")) {

					long stm = 0;
					long etm = 0;
					Date st = null;
					Date et = null;
					try {
						Iterable<Relationship> rsg = node.getRelationships(RelTypes.INSTANCE_OF);
						Relationship rs=null;
						Node otherNode=null;
						Iterator<Relationship> rsi = rsg.iterator();
						while(rsi.hasNext()){
							rs=rsi.next();
						}
						if(rs!=null){
							otherNode=rs.getOtherNode(node);
						}
						if (node.getProperty("startTime") != null
								&& node.getProperty("endTime") != null&&otherNode!=null) {
							st = getDate(node.getProperty("startTime")
									.toString());
							et = getDate(node.getProperty("endTime").toString());
							stm = st.getTime() - date.getTime();
							etm = et.getTime() - date.getTime();
							bw.newLine();
							bw.write(node.getProperty("name")+node.getProperty("invocationId").toString()+ "," + stm
									/ 1000 + "," + etm / 1000
									+ ","+node.getProperty("blockUUID")+","+ node.getProperty("invocationId")+","+otherNode.getProperty("name")+"__"+otherNode.getProperty("versionId")+"__"+otherNode.getProperty("versionnumber"));
						}

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void registerShutdownHook() {
		// TODO Auto-generated method stub
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
				newgraphDb.shutdown();
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void down() {
		tx.success();
		tx2.success();
		tx.finish();
		tx2.finish();

	}
/**
 * for test
 */
	public void getIndex() {

		String[] str = graphDb.index().nodeIndexNames();
		for (String st : str) {
			System.out.println(st);
		}
	}

	private void test(String num) {
		// Node node=graphDb
		// .findNodesByLabelAndProperty(null, "invocationId","5901"
		// ).iterator().next();

		String ESC_ID_INDEX_NAME = "esc-index";
		escIdIndex = graphDb.index().forNodes(ESC_ID_INDEX_NAME);

		IndexHits<Node> allIds = escIdIndex.get("invocationId", num);
		// Node node=allIds.getSingle();
		Iterator<Node> tor = allIds.iterator();
		while (tor.hasNext()) {
			Node node = (Node) tor.next();
			// str.add("VERSION_OF");
			// str.add("INCOMING");
			rulenode();
			createPic(node, rule);
			// System.out.println(node.getProperty("TYPE"));
		}
	}

	private void drawgantt(String num){

		String ESC_ID_INDEX_NAME = "esc-index";
		escIdIndex = graphDb.index().forNodes(ESC_ID_INDEX_NAME);

		IndexHits<Node> allIds = escIdIndex.get("invocationId", num);
		Iterator<Node> tor = allIds.iterator();
		Node newnode = null;
		while (tor.hasNext()) {
			Node node = (Node) tor.next();
			// str.add("VERSION_OF");
			// str.add("INCOMING");
			newnode=getNode(node);
			// System.out.println(node.getProperty("TYPE"));
		}
		
		final MyGanttChart chartCreator = new MyGanttChart();
		System.out.println("...Creating Dataset");
		chartCreator.setDataset(newnode);
		IntervalCategoryDataset dataset = chartCreator.createDataset();

		System.out.println("...Creating Chart");
		JFreeChart chart = chartCreator.createChart(dataset,num);

		String fileName = num+".jpg";

		System.out.println("...Saving the Chart");
		chartCreator.saveChart(chart, fileName);

		System.out.println("...Chart Created Successfully and Saved");
		System.out.println("Output Chart File Location: " + fileName);
	}
	private void rulenode() {
		// with version
		rule.add("INVOKED_BY");
		rule.add("INCOMING");
		rule.add("RUN_OF");
		rule.add("OUTGOING");
		rule.add("CONTAINED");
		rule.add("OUTGOING");
		rule.add("VERSION_OF");
		rule.add("OUTGOING");
		rule.add("USED");
		rule.add("OUTGOING");
		rule.add("USED");
		rule.add("INCOMING");
		rule.add("INSTANCE_OF");
		rule.add("OUTGOING");
		rule.add("WAS_GENERATED_BY");
		rule.add("INCOMING");
		rule.add("WAS_GENERATED_BY");
		rule.add("OUTGOING");

		
		//library
//		rule.add("REQUIRED");
//		rule.add("OUTGOING");
		
		
		
		
		// without version
//		 rule.add("INVOKED_BY");
//		 rule.add("INCOMING");
//		 rule.add("CONTAINED");
//		 rule.add("OUTGOING");
//		 rule.add("USED");
//		 rule.add("OUTGOING");
//		 rule.add("USED");
//		 rule.add("INCOMING");
//		 rule.add("WAS_GENERATED_BY");
//		 rule.add("INCOMING");
//		 rule.add("WAS_GENERATED_BY");
//		 rule.add("OUTGOING");

		//just workflowrun and the service run and the version of the workflow run and the service run.
//		rule.add("INVOKED_BY");
//		rule.add("INCOMING");
//		rule.add("RUN_OF");
//		rule.add("OUTGOING");
//		rule.add("CONTAINED");
//		rule.add("OUTGOING");
//		rule.add("VERSION_OF");
//		rule.add("OUTGOING");
//		rule.add("INSTANCE_OF");
//		rule.add("OUTGOING");
	}

	/**
	 * This method need a parameter node
	 *  which is a node reference 
	 *  from the old database and return type 
	 *  is a node reference from the new database.
	 *   This method will returns a reference
	 *    from the new database based on the node
	 *     from the old database.
	 * @param node
	 *            from the old DB
	 * @return the node from the new DB
	 */

	private Node getNode(Node node) {
		if (node == null) {
			System.out.println("error");
		}
		if (node.hasProperty("TYPE")) {
			String str = (String) node.getProperty("TYPE");
			switch (str) {
			case "Service Version":
				ServiceVersion sv = new ServiceVersion(node);
				return listNode.get("Service Version" + sv.getVersionId());
			case "Library":
				Library li = new Library(node);
				return listNode.get("Library" + li.getVersionId());
			case "Workflow Run":
				WorkflowRun wr = new WorkflowRun(node);
				return listNode.get("Workflow Run" + wr.getInvocationId());
			case "Transient Data":
				TransientData td = new TransientData(node);
				return listNode.get("Transient Data" + td.getId());
			case "DataVersion":
				DataVersion dv = new DataVersion(node);
				return listNode.get("DataVersion" + dv.getVersionId());
			case "Service Run":
				ServiceRun sr = new ServiceRun(node);
				return listNode.get("Service Run" + sr.getInvocationId()
						+ sr.getBlockUuid());
			case "Workflow Version":
				WorkflowVersion wv = new WorkflowVersion(node);
				return listNode.get("Workflow Version" + wv.getVersionId());
			case "User":
				User u = new User(node);
				return listNode.get("User" + u.getEscId());
			default:
				System.out.println("unknow type of node");
				break;
			}
		}
		return null;
	}

	/**
	 * newnode is a node reference from a new database . 
	 * node is a corresponding node reference from the old database.
	 *  Str represents the direction of the relationships 
	 *  and relationshiptype represents the type of the relationships.
	 *   This method is used to draw the node and its neighbor nodes 
	 *   and relationships, which are based on the direction and 
	 *   the type of the relationship.
	 * @param newnode
	 * @param node
	 * @param str
	 * @param relationshiptype
	 * @return
	 */
	private List<Node> picture(Node newnode, Node node, String str,
			RelTypes relationshiptype) {
		Iterable<Relationship> aimrelationships;
		List<Node> oldNodes = new LinkedList<Node>();

		if (str.equals("OUTGOING")) {
			aimrelationships = node.getRelationships(Direction.OUTGOING,
					relationshiptype);
			Iterator<Relationship> aimre = aimrelationships.iterator();
			while (aimre.hasNext()) {
				Relationship relship = aimre.next();
				Node endnode = relship.getEndNode();
				if (getNode(endnode) == null) {

					Node newendnode = addNode(endnode);
					newnode.createRelationshipTo(newendnode, relationshiptype);
					createPic(endnode, rule);
				} else {
					Node newendnode = addNode(endnode);
					if (!verifyRelationship(newnode, newendnode,
							Direction.OUTGOING, relationshiptype)) {
						newnode.createRelationshipTo(newendnode,
								relationshiptype);
						oldNodes.add(endnode);
					}
				}

			}
		} else if (str.equals("INCOMING")
				&& !node.getProperty("TYPE").equals("DataVersion")) {
			aimrelationships = node.getRelationships(Direction.INCOMING,
					relationshiptype);
			Iterator<Relationship> aimre = aimrelationships.iterator();
			while (aimre.hasNext()) {
				Relationship relship = aimre.next();
				Node startnode = relship.getStartNode();
				if (getNode(startnode) == null) {
					Node newstartnode = addNode(startnode);
					newstartnode
							.createRelationshipTo(newnode, relationshiptype);
					createPic(startnode, rule);
				} else {
					Node newstartnode = addNode(startnode);
					if (!verifyRelationship(newnode, newstartnode,
							Direction.INCOMING, relationshiptype)) {
						newstartnode.createRelationshipTo(newnode,
								relationshiptype);
						oldNodes.add(startnode);
					}
				}
			}
		}
		return oldNodes;
	}

	/**
	 *  This method aim to verify whether
	 *   the relationship has already been created or not 
	 *   based on the parameters. 
	 *   If the relationship is already created, 
	 *   it returns 'true', otherwise it returns 'false'.
	 * @param startnode
	 * @param endnode
	 * @param direstion
	 * @param relationshiptype
	 * @return
	 */
	private boolean verifyRelationship(Node startnode, Node endnode,
			Direction direstion, RelTypes relationshiptype) {
		for (Relationship neighbor : startnode.getRelationships(direstion,
				relationshiptype)) {
			if (neighbor.getOtherNode(startnode).equals(endnode))
				return true;
		}
		return false;

	}

	private void clearDb(String DB_PATH) {
		try {
			FileUtils.deleteRecursively(new File(DB_PATH));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Node> createPic(Node node, List<String> str) {
		if (node == null || str == null || str.size() % 2 == 1) {
			System.out.println("error");
		}

		Node newnode = addNode(node);
		List<Node> listNode = new LinkedList<Node>();
		for (int i = 0; i < str.size(); i = i + 2) {
			String relationType = str.get(i);
			switch (relationType) {
			case "RUN_OF":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.RUN_OF));
				break;
			case "USED":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.USED));
				break;
			case "INSTANCE_OF":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.INSTANCE_OF));
				break;
			case "REQUIRED":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.REQUIRED));
				break;
			case "CONTAINED":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.CONTAINED));
				break;
			case "WAS_GENERATED_BY":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.WAS_GENERATED_BY));
				break;
			case "VERSION_OF":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.VERSION_OF));
				break;
			case "INVOKED_BY":
				listNode.addAll(picture(newnode, node, str.get(i + 1),
						RelTypes.INVOKED_BY));
				break;
			default:
				System.out.println("error");
			}

		}
		return listNode;
	}

//	private List<Node> createmyPic(Node node, List<String> str) {
//		if (node == null || str == null || str.size() % 2 == 1) {
//			System.out.println("error");
//		}
//
//		Node newnode = addNode(node);
//		List<Node> listNode = new LinkedList<Node>();
//		for (int i = 0; i < str.size(); i = i + 2) {
//			String relationType = str.get(i);
//			switch (relationType) {
//			case "RUN_OF":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.RUN_OF));
//				break;
//			case "USED":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.USED));
//				break;
//			case "INSTANCE_OF":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.INSTANCE_OF));
//				break;
//			case "REQUIRED":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.REQUIRED));
//				break;
//			case "CONTAINED":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.CONTAINED));
//				break;
//			case "WAS_GENERATED_BY":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.WAS_GENERATED_BY));
//				break;
//			case "VERSION_OF":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.VERSION_OF));
//				break;
//			case "INVOKED_BY":
//				listNode.addAll(picture(newnode, node, str.get(i + 1),
//						RelTypes.INVOKED_BY));
//
//				break;
//			default:
//				System.out.println("error");
//			}
//
//		}
//		return listNode;
//	}

	public static void main(String[] arg) {
		if (arg.length != 2) {
			System.out.println("java -jar param1 param2");
			System.out.println("param1 database address,param2 invocationId.");
			System.out.println("For Example:graph.db 8101"); 
		}else{
		final String DB_LOC = arg[0];
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_LOC);
		List<String> nums=new ArrayList<String>();
		nums.add(arg[1]);
//		nums.add("5916");
//		nums.add("5939");
//		nums.add("5948");
//		nums.add("5958");
//		nums.add("5970");
//		nums.add("5992");
//		nums.add("6767");
//		nums.add("8101");
//		nums.add("8127");
//		nums.add("8155");
//		nums.add("8540");		
//		nums.add("8542");
//		nums.add("8414");		
//		nums.add("8718");
//		nums.add("8737");
//		nums.add("8157");
//		nums.add("8809");
//		nums.add("8829");
//		nums.add("8872");
//		nums.add("8925");
//		nums.add("8945");
//		nums.add("8991");
//		nums.add("9014");
//		nums.add("9053");
//		nums.add("9665");
//		nums.add("9693");
//		nums.add("9863");
//		nums.add("9881");
//		nums.add("10146");
//		nums.add("10207");
		
		for(int i=0;i<nums.size();i++){
			newDB(nums.get(i),graphDb);
		}
		
		}
	}
	
	private static void newDB(String num,GraphDatabaseService graphDb){
		CreateGraph createg = new CreateGraph(graphDb);

		createg.setUp(num+".db");

		// createg.getIndex();
		createg.test(num);
		// createg.deleteWorkflowRun();
		createg.outputTime();
		createg.generatServiceRunTime(num);

		createg.drawgantt(num);
		createg.down();
		System.out.println(num+" finish");
	}

	/**
	 * This method need a parameter node 
	 * which is a node reference from the old database 
	 * and return type is a node reference from the new database.
	 *  The method is used to create a new node for 
	 *  the new database based on the node from the old database.
	 *   Firstly the method will verify the type of the node 
	 *   and casts the node to the corresponding type node and
	 *    then it will get all the information from the node and
	 *     store the information for the new node.
	 * @param node
	 * @return
	 */
	private Node addNode(Node node) {
		Node n = getNode(node);
		if (n != null) {
			return n;
		}
		if (node.hasProperty("TYPE")) {
			String str = (String) node.getProperty("TYPE");

			switch (str) {
			case "Service Version":
				ServiceVersion sv = new ServiceVersion(node);
				return createServiceNode(sv);
			case "Library":
				Library li = new Library(node);
				return createLibraryNode(li);
			case "Workflow Run":
				WorkflowRun wr = new WorkflowRun(node);
				return createWorkflowRunNode(wr);
			case "Transient Data":
				TransientData td = new TransientData(node);
				return createTransientDataNode(td);
			case "DataVersion":
				DataVersion dv = new DataVersion(node);
				return createDataVersionNode(dv);
			case "Service Run":
				ServiceRun sr = new ServiceRun(node);
				return createServiceRunNode(sr);
			case "Workflow Version":
				WorkflowVersion wv = new WorkflowVersion(node);
				return createWorkflowVersionNode(wv);
			case "User":
				User u = new User(node);
				return createUserNode(u);
			default:
				System.out.println("unknow type of node");
				return null;
			}
		}
		return null;

	}

	private void outputTime() {
		Collections.sort(listDate);
		System.out.println(listDate.get(0));
		date = listDate.get(0);
		System.out.println(listDate.get(listDate.size() - 1));

	}

	private Node createServiceNode(ServiceVersion serviceversion) {
		if (listNode.containsKey("Service Version"
				+ serviceversion.getVersionId())) {
			return listNode.get("Service Version"
					+ serviceversion.getVersionId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("ServiceVersion");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", serviceversion.getEscId());
		node23.setProperty("TYPE", "ServiceVersion");
		node23.setProperty("escid", serviceversion.getEscId());
		node23.setProperty("name", serviceversion.getName());
		node23.setProperty("versionId", serviceversion.getVersionId());
		try {
			node23.setProperty("versionnumber", serviceversion.getVersionNum());
		} catch (ClassCastException e) {
			System.out.println("The name of the service version("
					+ serviceversion.toString() + " versionId:"
					+ serviceversion.getVersionId() + ") cannot be casted");
		}
		listNode.put("Service Version" + serviceversion.getVersionId(), node23);
		return node23;
	}

	private Node createLibraryNode(Library library) {
		if (listNode.containsKey("Library" + library.getVersionId())) {
			return listNode.get("Library" + library.getVersionId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("Library");
		node23.addLabel(myLabel);
		node23.setProperty("identifier",
				library.getEscId() + "-" + library.getVersionNum());
		node23.setProperty("TYPE", "Library");
		node23.setProperty("escid", library.getEscId());
		node23.setProperty("name", library.getName());
		node23.setProperty("versionId", library.getVersionId());
		node23.setProperty("versionnumber", library.getVersionNum());
		listNode.put("Library" + library.getVersionId(), node23);
		return node23;

	}

	private Node createWorkflowRunNode(WorkflowRun workflowrun) {
		if (listNode
				.containsKey("Workflow Run" + workflowrun.getInvocationId())) {
			return listNode.get("Workflow Run" + workflowrun.getInvocationId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("WorkflowRun");
		node23.addLabel(myLabel);

		node23.setProperty("identifier", workflowrun.getInvocationId());
		node23.setProperty("TYPE", "WorkflowRun");
		if (workflowrun.getName() != null) {
			node23.setProperty("name", workflowrun.getName());
		}

		// node23.setProperty("escid", workflowrun.getEscId());

		node23.setProperty("invocationId", workflowrun.getInvocationId());
		listNode.put("Workflow Run" + workflowrun.getInvocationId(), node23);
		return node23;
	}

	private Node createTransientDataNode(TransientData transientdata) {
		if (listNode.containsKey("Transient Data" + transientdata.getId())) {
			return listNode.get("Transient Data" + transientdata.getId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("TransientData");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", transientdata.getId() + "-"
				+ transientdata.getHashValue());

		node23.setProperty("TYPE", "TransientData");
		node23.setProperty("name", transientdata.getName());
		node23.setProperty("id", transientdata.getId());
		node23.setProperty("DataSize", transientdata.getDataSize());
		node23.setProperty("DataType", transientdata.getDataType());
		node23.setProperty("HashValue", transientdata.getHashValue());
		listNode.put("Transient Data" + transientdata.getId(), node23);
		return node23;
	}

	private Node createDataVersionNode(DataVersion dataversion) {
		if (listNode.containsKey("DataVersion" + dataversion.getVersionId())) {
			return listNode.get("DataVersion" + dataversion.getVersionId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("DateVersion");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", dataversion.getEscId() + "-"
				+ dataversion.getDocumentId());
		node23.setProperty("TYPE", "DateVersion");
		node23.setProperty("escid", dataversion.getEscId());
		node23.setProperty("name", dataversion.getName());
		node23.setProperty("versionId", dataversion.getVersionId());
		node23.setProperty("versionnumber", dataversion.getVersionNum());
		node23.setProperty("documetId", dataversion.getDocumentId());
		listNode.put("DataVersion" + dataversion.getVersionId(), node23);
		return node23;
	}

	private Node createServiceRunNode(ServiceRun servicerun) {
		if (listNode.containsKey("Service Run" + servicerun.getInvocationId()
				+ servicerun.getBlockUuid())) {
			return listNode.get("Service Run" + servicerun.getInvocationId()
					+ servicerun.getBlockUuid());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("ServiceRun");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", servicerun.getInvocationId() + "-"
				+ servicerun.getBlockUuid());
		node23.setProperty("TYPE", "ServiceRun");
		// node23.setProperty("escid", servicerun.getEscId());
		node23.setProperty("name", servicerun.getName());
		node23.setProperty("invocationId", servicerun.getInvocationId());
		node23.setProperty("blockUUID", servicerun.getBlockUuid());
		try {
			String time = servicerun.getStartTime();
			node23.setProperty("startTime", time);
			listDate.add(getDate(time));
		} catch (org.neo4j.graphdb.NotFoundException e) {
			System.out.println("strat time is null");
		}

		try {
			String time = servicerun.getEndTime();
			node23.setProperty("endTime", servicerun.getEndTime());
			listDate.add(getDate(time));
		} catch (org.neo4j.graphdb.NotFoundException e) {

		}
		listNode.put(
				"Service Run" + servicerun.getInvocationId()
						+ servicerun.getBlockUuid(), node23);
		// System.out.println( servicerun.getPropertiesData());
		// node23.setProperty("propertiesData",servicerun.getPropertiesData());
		return node23;
	}

	private Node createWorkflowVersionNode(WorkflowVersion workflowrun) {
		if (listNode.containsKey("Workflow Version"
				+ workflowrun.getVersionId())) {
			return listNode
					.get("Workflow Version" + workflowrun.getVersionId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("WorkflowVersion");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", workflowrun.getVersionId() + "-"
				+ workflowrun.getVersionNum());
		node23.setProperty("TYPE", "WorkflowVersion");
		node23.setProperty("escid", workflowrun.getEscId());
		node23.setProperty("name", workflowrun.getName());
		node23.setProperty("versionId", workflowrun.getVersionId());
		node23.setProperty("versionnumber", workflowrun.getVersionNum());
		listNode.put("Workflow Version" + workflowrun.getVersionId(), node23);
		return node23;
	}

	private Node createUserNode(User user) {
		if (listNode.containsKey("User" + user.getEscId())) {
			return listNode.get("User" + user.getEscId());
		}
		Node node23 = newgraphDb.createNode();
		Label myLabel = DynamicLabel.label("User");
		node23.addLabel(myLabel);
		node23.setProperty("identifier", user.getName() + "-" + user.getEscId());
		node23.setProperty("TYPE", "User");
		node23.setProperty("escid", user.getEscId());
		node23.setProperty("name", user.getName());
		listNode.put("User" + user.getEscId(), node23);
		return node23;
	}

}
