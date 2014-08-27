package ExtractWorkflowRun.ExtractWorkflowRun;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.IntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;



public class MyGanttChart {

	private enum RelTypes implements RelationshipType {
		// WAS_DERIVED_FROM, WAS_INFORMED_BY, USED, WAS_GENERATED_BY, HAD,
		// WITH_PLAN, WAS_ATTRIBUTED_TO;
		RUN_OF, USED, INSTANCE_OF, REQUIRED, CONTAINED, WAS_GENERATED_BY, VERSION_OF, INVOKED_BY;
	}

	private int numbersr=0;
	private TaskSeriesCollection collection = new TaskSeriesCollection();

	TaskSeries seriesOne = new TaskSeries("time");

	public void addseries(TaskSeries task, Node node) {
		/** Adding data in this series **/
		task.add(new Task(node.getProperty("name").toString() + "_"
				+ node.getProperty("invocationId").toString(), getDate(node
				.getProperty("startTime").toString()), getDate(node
				.getProperty("endTime").toString())));
		System.out.println(node.getProperty("name").toString() + "_"
				+ node.getProperty("invocationId") + "_"
				+ node.getProperty("startTime").toString()+node
				.getProperty("endTime").toString());
	}

	public IntervalCategoryDataset createDataset() {
		collection.add(seriesOne);
		return collection;
	}

	public void setDataset(Node node) {

		if (!node.getProperty("TYPE").equals("WorkflowRun")) {
			System.out.println("unknow type");
		}

		Iterable<Relationship> relationshiptypes = node.getRelationships(
				Direction.OUTGOING, RelTypes.CONTAINED);
		Iterator<Relationship> rt = relationshiptypes.iterator();
		final List<Date> datesr = new ArrayList<Date>();
		final List<Date> datesrend = new ArrayList<Date>();
		final List<MyNode> myNodes = new ArrayList<MyNode>();
		while (rt.hasNext()) {
			Relationship relationtype = rt.next();
			Node otherNode = relationtype.getOtherNode(node);
			MyNode mynode = new MyNode();

			Date sDate = getDate(otherNode.getProperty("startTime").toString());
			Date eDate = getDate(otherNode.getProperty("endTime").toString());
			mynode.setDate(sDate);
			mynode.setNode(otherNode);
			myNodes.add(mynode);
			datesr.add(sDate);
			datesrend.add(eDate);
		}
		Collections.sort(myNodes);
		Collections.sort(datesr);
		Collections.sort(datesrend);
		
		Task task = null;
		if (myNodes.size() != 0) {
			System.out.println(node.getProperty("name").toString() + "_"
					+ node.getProperty("invocationId").toString()
					+ datesr.get(0));
			task = new Task(node.getProperty("name").toString() + "_"
					+ node.getProperty("invocationId").toString(),
					datesr.get(0), datesrend.get(datesrend.size() - 1));
			seriesOne.add(task);
			numbersr++;
			for (int i = 0; i < myNodes.size(); i++) {
				MyNode mn = myNodes.remove(0);
				addseries(seriesOne, mn.getNode());
				numbersr++;
			}

		}

		Iterable<Relationship> relationshipinvoke = node.getRelationships(
				Direction.INCOMING, RelTypes.INVOKED_BY);
		Iterator<Relationship> rti = relationshipinvoke.iterator();
		while (rti.hasNext()) {
			Relationship relationtype = rti.next();
			Node otherNode = relationtype.getOtherNode(node);
			setDataset(otherNode);
		}
		/**
		 * Creating a task series And adding planned tasks dates on the series.
		 */
		// TaskSeries seriesOne = new TaskSeries("Planned Implementation");
		//
		// /** Adding data in this series **/
		// seriesOne.add(new Task("Sanjaal Domain Registration",
		// new SimpleTimePeriod(makeDate(10, Calendar.JUNE, 2007),
		// makeDate(15, Calendar.JUNE, 2007))));
		//
		// seriesOne.add(new Task("Feature Addition - Java Blog",
		// new SimpleTimePeriod(makeDate(9, Calendar.JULY, 2007),
		// makeDate(19, Calendar.JULY, 2007))));
		//
		// seriesOne.add(new Task("Feature Addition - PHPBB Forum",
		// new SimpleTimePeriod(makeDate(10, Calendar.AUGUST, 2007),
		// makeDate(15, Calendar.AUGUST, 2007))));
		//
		// seriesOne.add(new Task("Feature Addition - Tagged Mails",
		// new SimpleTimePeriod(makeDate(6, Calendar.MAY, 2007), makeDate(
		// 30, Calendar.MAY, 2007))));
		//
		// seriesOne.add(new Task("Feature Addition - H1B Visa Portal",
		// new SimpleTimePeriod(makeDate(2, Calendar.JUNE, 2007),
		// makeDate(2, Calendar.JUNE, 2007))));
		//
		// seriesOne.add(new Task("Feature Addition - Events Gallery",
		// new SimpleTimePeriod(makeDate(3, Calendar.JUNE, 2007),
		// makeDate(31, Calendar.JULY, 2007))));
		//
		// seriesOne.add(new Task("Google Adsense Integration",
		// new SimpleTimePeriod(makeDate(1, Calendar.AUGUST, 2007),
		// makeDate(8, Calendar.AUGUST, 2007))));
		//
		// seriesOne.add(new Task("Adbrite Advertisement Integration",
		// new SimpleTimePeriod(makeDate(10, Calendar.AUGUST, 2007),
		// makeDate(10, Calendar.AUGUST, 2007))));
		//
		// seriesOne.add(new Task("InfoLink Advertisement Integration",
		// new SimpleTimePeriod(makeDate(12, Calendar.AUGUST, 2007),
		// makeDate(12, Calendar.SEPTEMBER, 2007))));
		//
		// seriesOne.add(new Task("Feature Testing", new SimpleTimePeriod(
		// makeDate(13, Calendar.SEPTEMBER, 2007), makeDate(31,
		// Calendar.OCTOBER, 2007))));
		//
		// seriesOne.add(new Task("Public Release", new
		// SimpleTimePeriod(makeDate(
		// 1, Calendar.NOVEMBER, 2007), makeDate(15, Calendar.NOVEMBER,
		// 2007))));
		//
		// seriesOne.add(new Task("Post Release Bugs Collection",
		// new SimpleTimePeriod(makeDate(28, Calendar.NOVEMBER, 2007),
		// makeDate(30, Calendar.NOVEMBER, 2007))));
		//
		// /**
		// * Creating another task series
		// */
		// TaskSeries seriesTwo = new TaskSeries("Actual Implementation");
		//
		// /** Adding data in this series **/
		// seriesTwo.add(new Task("Sanjaal Domain Registration",
		// new SimpleTimePeriod(makeDate(11, Calendar.JUNE, 2007),
		// makeDate(14, Calendar.JUNE, 2007))));
		//
		// seriesTwo.add(new Task("Feature Addition - Java Blog",
		// new SimpleTimePeriod(makeDate(11, Calendar.JULY, 2007),
		// makeDate(19, Calendar.JULY, 2007))));
		//
		// seriesTwo.add(new Task("Feature Addition - PHPBB Forum",
		// new SimpleTimePeriod(makeDate(10, Calendar.AUGUST, 2007),
		// makeDate(17, Calendar.AUGUST, 2007))));
		//
		// seriesTwo.add(new Task("Feature Addition - Tagged Mails",
		// new SimpleTimePeriod(makeDate(7, Calendar.MAY, 2007), makeDate(
		// 1, Calendar.JUNE, 2007))));
		//
		// seriesTwo.add(new Task("Feature Addition - H1B Visa Portal",
		// new SimpleTimePeriod(makeDate(2, Calendar.JUNE, 2007),
		// makeDate(4, Calendar.JUNE, 2007))));
		//
		// seriesTwo.add(new Task("Feature Addition - Events Gallery",
		// new SimpleTimePeriod(makeDate(3, Calendar.JUNE, 2007),
		// makeDate(13, Calendar.JULY, 2007))));
		//
		// seriesTwo.add(new Task("Google Adsense Integration",
		// new SimpleTimePeriod(makeDate(2, Calendar.AUGUST, 2007),
		// makeDate(7, Calendar.AUGUST, 2007))));
		//
		// seriesTwo.add(new Task("Adbrite Advertisement Integration",
		// new SimpleTimePeriod(makeDate(10, Calendar.AUGUST, 2007),
		// makeDate(11, Calendar.AUGUST, 2007))));
		//
		// seriesTwo.add(new Task("InfoLink Advertisement Integration",
		// new SimpleTimePeriod(makeDate(13, Calendar.AUGUST, 2007),
		// makeDate(15, Calendar.SEPTEMBER, 2007))));
		//
		// seriesTwo.add(new Task("Feature Testing", new SimpleTimePeriod(
		// makeDate(13, Calendar.SEPTEMBER, 2007), makeDate(3,
		// Calendar.NOVEMBER, 2007))));
		//
		// seriesTwo.add(new Task("Public Release", new
		// SimpleTimePeriod(makeDate(
		// 4, Calendar.NOVEMBER, 2007), makeDate(15, Calendar.NOVEMBER,
		// 2007))));
		//
		// seriesTwo.add(new Task("Post Release Bugs Collection",
		// new SimpleTimePeriod(makeDate(28, Calendar.NOVEMBER, 2007),
		// makeDate(3, Calendar.DECEMBER, 2007))));

	}

	// private static Date makeDate(final int day, final int month, final int
	// year) {
	//
	// final Calendar calendar = Calendar.getInstance();
	// calendar.set(year, month, day);
	// final Date result = calendar.getTime();
	// return result;
	//
	// }

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

	/**
	 * Creates a Gantt chart based on input data set
	 */
	public JFreeChart createChart(final IntervalCategoryDataset dataset,String num) {
		final JFreeChart chart = ChartFactory.createGanttChart(
				"Gantt Chart -For "+num+" top workflowrun", // chart
															// title
				"name", // domain axis label
				"Date", // range axis label
				dataset, // data
				true, // include legend
				true, // tooltips
				false // urls
				);
		System.out.println(collection.getSeriesCount());

		return chart;

	}

	public void saveChart(JFreeChart chart, String fileLocation) {
		String fileName = fileLocation;
		try {
			/**
			 * This utility saves the JFreeChart as a JPEG First Parameter:
			 * FileName Second Parameter: Chart To Save Third Parameter: Height
			 * Of Picture Fourth Parameter: Width Of Picture
			 */
			ChartUtilities.saveChartAsJPEG(new File(fileName), chart, 1000,
					numbersr*30);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problem occurred creating chart.");
		}
	}

	/**
	 * Testing the Gantt Chart Creation
	 */
	public static void main(final String[] args) {
		//
		// final MyGanttChart chartCreator = new MyGanttChart();
		// System.out.println("...Creating Dataset");
		// IntervalCategoryDataset dataset = chartCreator.createDataset();
		//
		// System.out.println("...Creating Chart");
		// JFreeChart chart = chartCreator.createChart(dataset);
		//
		// String fileName = "C:/temp/myGantChartDemo.jpg";
		//
		// System.out.println("...Saving the Chart");
		// chartCreator.saveChart(chart, fileName);
		//
		// System.out.println("...Chart Created Successfully and Saved");
		// System.out.println("Output Chart File Location: " + fileName);

	}
}
