
// INFO3403, W2017 - Lab5
//Answered by Peter Adamson

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;


import org.apache.calcite.adapter.csv.CsvSchemaFactory; 
import org.apache.calcite.config.Lex;
import org.apache.calcite.jdbc.CalciteConnection;
import org.apache.calcite.plan.Contexts;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.rules.FilterMergeRule;
import org.apache.calcite.rel.rules.FilterProjectTransposeRule;
import org.apache.calcite.rel.rules.LoptOptimizeJoinRule;
import org.apache.calcite.rel.rules.ProjectMergeRule;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.tools.RelRunners;
import org.apache.calcite.tools.RuleSets;

import com.google.common.collect.ImmutableMap;

public class RelAlgebraDemo {
	
	 private final boolean verbose= true;

	 Connection calConn = null;
	 
	 public static void main(String[] args) { 
	    new RelAlgebraDemo().runAll();
	}

	 //--------------------------------------------------------------------------------------
	 
	 /**
	  * Create a relational algebra expression for the query:
	  * Show all the records of the employees table 
	  * 
	  * Then execute the expression.
	  */
	 private void exercise0(RelBuilder builder) {
		 System.out.println("Running: Show all the records of the employees table");
		 
		 // write your relational algebra expression here
		 builder
		 .scan("EMPLOYEE");
		    
		 
		 //keep the following code template to build, show and execute the relational algebra expression
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getString(2));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 
	 /**
	  * Create a relational algebra expression for the query:
	  * Show the name, salary of the employees who earn earn more than 50k
	  * 
	  * Then execute the expression.
	  */
	 private void exercise1(RelBuilder builder) {
		 System.err.println("Running: select name, salary from EMPLOYEE where salary > 50000");
		 builder
		 .scan("EMPLOYEE")
		 .filter(  builder.call(SqlStdOperatorTable.GREATER_THAN, builder.field("SALARY"), builder.literal(50000) ) )
		 .project(builder.field("NAME"), builder.field("SALARY"));
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
			  
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getString(1)+ " " + rs.getFloat(2));
			 }
			 rs.close();
			 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }

	
	 /**
	  * Create a relational algebra expression for the query:
	  * Show the name of the employees who work in Sales or Marketing department
	  * 
	  * Then execute the expression.
	  */
	 private void exercise2(RelBuilder builder) {
		 System.err.println("select e.name, d.name from EMPLOYEE e, DEPT d where e.deptno = d.deptno AND (e.deptno=10 OR e.deptno=20)");
		 builder
		 .scan("EMPLOYEE").as("e")
		 .scan("DEPT").as("d")
		 
		 //natural join
		 .join(JoinRelType.INNER, "DEPTNO")
		 
		 .filter(builder.or(builder.equals(builder.field("DEPTNO"), builder.literal(10)),builder.equals(builder.field("DEPTNO"),builder.literal(20))))
	
		 .project(builder.field("e","NAME"),builder.field("d","NAME"));
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
			  
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getString(1) + " " + rs.getString(2));
			 }
			 rs.close();
			 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 
	
	 /**
	  * Create a relational algebra expression for the query:
	  * Show the name of the employees, the course titles and the date of completion of the courses 
	  * for which the employees received certificates
	  * 
	  * Then execute the expression.
	  */
	 private void exercise3(RelBuilder builder) {
		 System.err.println("SELECT e.NAME, c.TITLE, ce.COMPLETIONDATE FROM EMPLOYEE e, COURSE c, CERTIFICATE ce WHERE e.EMPID = ce.EMPID and ce.COURSEID = c.COURSEID ");
		 builder
		 .scan("EMPLOYEE").as("e")
		 .scan("CERTIFICATE").as("ce")
		 .scan("COURSE").as("c")
		 .join(JoinRelType.INNER)
		 .join(JoinRelType.INNER)
		 .filter(builder.equals(builder.field("e", "EMPID"),builder.field("ce", "EMPID")))
		 .filter(builder.equals(builder.field("ce", "COURSEID"),builder.field("c", "COURSEID")))
		 
		d("DEPTNO"),builder.literal(20))))
	
		 .project(builder.field("e","NAME"),builder.field("c","TITLE"),builder.field("ce","COMPLETIONDATE"));
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
			  
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getDate(3));
			 }
			 rs.close();
			 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 } 
	 }
	 
	 
	 //---------------------------------------------------------------------------------------	
	 //---------------------------------------------------------------------------------------
	 public void runAll() {
		 // Create a builder. The config contains a schema mapped
		 final FrameworkConfig config = buildConfig();  
		 final RelBuilder builder = RelBuilder.create(config);
		 
		 for (int r = 0; r <= 7; r++) {
			 //runExample(builder, r);
		 }
		 
		 for (int i = 0; i <= 3; i++) {
		 	 runExercises(builder, i);
		 }
	 }

	 
	 // Running the examples
	 private void runExercises(RelBuilder builder, int i) {
		 System.out.println("---------------------------------------------------------------------------------------");
		 switch (i) {
		 	 case 1:
		 		 exercise1(builder);
		 		 break;
		 	case 2:
		 		 exercise2(builder);
		 		 break;
		 	case 3:
		 		 exercise3(builder);
		 		 break;
		 }
	 }
	 
	 // Running the examples
	 private void runExample(RelBuilder builder, int i) {
		 System.out.println("---------------------------------------------------------------------------------------");
		 switch (i) {
			 case 0:
				 example0(builder);
				 break;
			 case 1:
				 example1(builder);
				 break;
			 case 2:
				 example2(builder);
				 break;
			 case 3:
				 example3(builder);
				 break;
			 case 4:
				 example4(builder); 
				 break;
			 case 5:
				 example5(builder);
				 break;
			 case 6:
				 example6(builder);
				 break;
			 case 7:
				 example7(builder);
				 break;
			 default:
				 throw new AssertionError("unknown example " + i);
		 }
	 }

	//---------------------------------------------------------------------------------------
	

	 /**
	  * TABLE SCAN
	  * Creates a relational algebra expression for the query:
	  * Running: Show the details of the courses
	  */
	 private void example0(RelBuilder builder) {
		 System.err.println("Running: select * from COURSE");
		 builder
		 .scan("COURSE");
			  
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
			  
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getString(2));
			 }
			 rs.close();
			 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }

	 
	 /**
	  * PROJECTION
	  * Creates a relational algebra expression for the query:
	  * Show the title of the course where courseid = 2
	  */
	 private void example1(RelBuilder builder) {
		 System.err.println("\nRunning: Show the title of the course where courseid = 2");
		 builder
		 .scan("COURSE")
		 .filter(  builder.equals(builder.field("COURSEID"), builder.literal(2))  )
		 // or
		 //.filter(  builder.call(SqlStdOperatorTable.EQUALS, builder.field("COURSEID"), builder.literal(2) ) )
		 .project(builder.field("TITLE"));
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getString(1));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 
	 /**
	  * SELECTION
	  * Creates a relational algebra expression for the query:
	  * Show the details of the courses where courseid > 2
	  */
	 private void example2(RelBuilder builder) {
		 System.err.println("\nRunning: Show the details of the courses where courseid > 2");
		 builder
		 .scan("COURSE")
		 .filter(  builder.call(SqlStdOperatorTable.GREATER_THAN, builder.field("COURSEID"), builder.literal(2) )  
				 )
		 .project(builder.field("COURSEID"), builder.field("TITLE"));
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getString(2));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }

	 
	 /**
	  * ORDER BY
	  * Creates a relational algebra expression for the query:
	  * Show the details of the first 5 courses sorted by the course [in descending order] [offset 2 limit 5]
	  */
	 private void example3(RelBuilder builder) {
		 System.err.println("\nRunning: Select * from COURSE order by COURSEID limit 5");
		 builder
		 .scan("COURSE")
		 .sort(  builder.field("COURSEID")  )  
		 //.sort( builder.desc( builder.field("COURSEID"))  )    // in descending order
		 .limit(2, 3) // offset 2, limit 3
		 .project(builder.field("COURSEID"), builder.field("TITLE"));
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getString(2));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 

	 /**
	  * GROUP BY HAVING
	  * Creates a relational algebra expression for the query:
	  * Show the number of courses in each course category [ where number of courses is greater than 1]
	  *
	  * SELECT CATID, count(*) AS C, 
	  * FROM COURSE
	  * GROUP BY CATID
	  * HAVING C > 1
	  */
	 private void example4(RelBuilder builder) {
		 System.err.println("\nRunning: Show the number of courses in each course category where number of courses is greater than 1");
		 builder
		 .scan("COURSE")
		 .aggregate(builder.groupKey("CATEGORYID"),
		            // builder.count(false, "C")
				    // or
				    builder.count(false, "C", builder.field("COURSEID") )
		            )
		 .filter( builder.call(SqlStdOperatorTable.GREATER_THAN, builder.field("C"), builder.literal(1)));
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getInt(2));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }

	 /**
	  * UNION
	  * Creates a relational algebra expression for the query:
	  * Show all categories from COURSE and CCATEGORY
	  *
	  * SELECT CATEGORYID FROM COURSE 
	  * Union
	  * SELECT CATID from CCATEGORY
	  */
	 private void example5(RelBuilder builder) {
		 System.err.println("\nRunning: Show all categories from COURSE and CCATEGORY");
		 builder
		 .scan("COURSE").project(builder.field("CATEGORYID"))
		 .scan("CCATEGORY").project(builder.field("CATID"))
		 .union(true, 1);
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
		 
		 
	 }
	 
	 
	 /**
	  * CROSS PRODUCT
	  * Creates a relational algebra expression for the query:
	  *
	  * SELECT * FROM COURSE, CCATEGORY
	  */
	 private void example6(RelBuilder builder) {
		 System.err.println("\nRunning: SELECT * FROM COURSE, CCATEGORY");
		 builder
		 .scan("COURSE")
		 .scan("CCATEGORY")
		 .join(JoinRelType.INNER);
		
		    
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getInt(1)+ " " + rs.getString(2)+ " " +rs.getInt(3) + " " + rs.getInt(4)+ " " + rs.getString(5));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 
	 
	 
	 /**
	  * INNER JOIN
	  * Creates a relational algebra expression for the query:
	  * Show the title of each course along with the name of the course category
	  *
	  * SELECT TITLE, CATNAME
	  * FROM COURSE c, CCATEGORY g
	  * WHERE c.CATEGORYID = g.CATID

	  */
	 private void example7(RelBuilder builder) {
		 System.err.println("\nRunning: Show the title of each course along with the name of the course category");
		 builder
		 .scan("COURSE").as("c")
		 .scan("CCATEGORY").as("g")
		 .join(JoinRelType.INNER)
		 .filter( builder.equals(builder.field("c", "CATEGORYID"), builder.field("g", "CATID")))
		 // Syntax:.filter (predicate1, predicate2);  where "," implies AND
		 .project(builder.field("TITLE"), builder.field("CATNAME"));
		 
		 final RelNode node = builder.build();
		 if (verbose) {
			 System.out.println(RelOptUtil.toString(node));
		 }
		    
		 // execute the query plan
		 try  {
			 final PreparedStatement preparedStatement = RelRunners.run(node, calConn);
			 ResultSet rs =  preparedStatement.executeQuery();
			 while (rs.next()) {
				 System.out.println(rs.getString(1)+ " -> " + rs.getString(2));
			 }
			 rs.close();	 
		 } catch (SQLException e) {
			 // TODO Auto-generated catch block
			 e.printStackTrace();
		 }
	 }
	 
	 
	 
	 
	 
	 
	 // example join with union/intersection
	 
	 
	
	 
	 /**
		   * Sometimes the stack becomes so deeply nested it gets confusing. To keep
		   * things straight, you can remove expressions from the stack. For example,
		   * here we are building a bushy join:
		   *
		   * <pre>
		   *                join
		   *              /      \
		   *         join          join
		   *       /      \      /      \
		   * CUSTOMERS ORDERS LINE_ITEMS PRODUCTS
		   * </pre>
		   *
		   * <p>We build it in three stages. Store the intermediate results in variables
		   * `left` and `right`, and use `push()` to put them back on the stack when it
		   * is time to create the final `Join`.
		   */
		  private void example8(RelBuilder builder) {
			  System.out.println("Running exampleDoesNotRun");
		    final RelNode left = builder
		        .scan("CUSTOMERS")
		        .scan("ORDERS")
		        .join(JoinRelType.INNER, "ORDER_ID")
		        .build();

		    final RelNode right = builder
		        .scan("LINE_ITEMS")
		        .scan("PRODUCTS")
		        .join(JoinRelType.INNER, "PRODUCT_ID")
		        .build();

		     builder
		        .push(left)
		        .push(right)
		        .join(JoinRelType.INNER, "ORDER_ID");
		     
		     final RelNode node = builder.build();
		     if (verbose) {
		       System.out.println(RelOptUtil.toString(node));
		     }
		  }
		  
		 
		  
		  /** Tests {@link org.apache.calcite.tools.RelRunner} for a table scan + filter
		   * query. */
		  private void exampleQueryOptimization(RelBuilder builder) {
			  System.out.println("Running example6");
		    // Equivalent SQL:
		    //   SELECT * FROM DEPT WHERE DEPTNO = 20
		    RelNode root =
		        builder.scan("DEPTS")
		            .filter(
		                builder.equals(builder.field("DEPTNO"), builder.literal(20)))
		            .build();

		    System.out.println("Before optimization..");
		    System.out.println(RelOptUtil.toString(root));
		    
		    RelRoot qroot = RelRoot.of(root, SqlKind.SELECT);
		    
		   //---------------------------------------------------------------
		   /* RelOptPlanner planner = new VolcanoPlanner();
		    
		    Program program = Programs.ofRules(
		    		FilterProjectTransposeRule.INSTANCE,
		    		ProjectMergeRule.INSTANCE,
		    		FilterMergeRule.INSTANCE,
		    		LoptOptimizeJoinRule.INSTANCE
		    		);
		    		
		    RelTraitSet traitset = planner.emptyTraitSet();
		    //http://logic.stanford.edu/dataintegration/
		    //http://web.cs.ucdavis.edu/~green/courses/ecs165a-w11/8-query.pdf
		    RelNode optimizedRoot = program.run(planner, qroot.rel, traitset);
		    
		   */
		   //---------------------------------------------------------------
		    
		    //---------------------------------------------------------------
		    HepProgramBuilder hepBuilder =  HepProgram.builder();
		    //hepBuilder.addRuleInstance(FilterProjectTransposeRule.INSTANCE);
		    HepProgram heprogram = hepBuilder.build();
		    RelOptPlanner planner = new  HepPlanner (heprogram);
		    planner.setRoot(root);
		    
		    Program program = Programs.ofRules(
		    		FilterProjectTransposeRule.INSTANCE,
		    		ProjectMergeRule.INSTANCE,
		    		FilterMergeRule.INSTANCE,
		    		LoptOptimizeJoinRule.INSTANCE
		    		);
		    RelTraitSet traitset = planner.emptyTraitSet();
		    //http://logic.stanford.edu/dataintegration/
		    //http://web.cs.ucdavis.edu/~green/courses/ecs165a-w11/8-query.pdf
		    RelNode optimizedRoot = program.run(planner, qroot.rel, traitset);
		   
		    
		  //---------------------------------------------------------------
		    System.out.println("After optimization..");
		    System.out.println(RelOptUtil.toString(optimizedRoot));
		    		
		    // Note that because the table has been resolved in the RelNode tree
		    // we do not need to supply a "schema" as context to the runner.
		    try  {
		      //final PreparedStatement preparedStatement = RelRunners.run(root);
		      final PreparedStatement preparedStatement = RelRunners.run(optimizedRoot, calConn);
		      ResultSet rs =  preparedStatement.executeQuery();
		      while (rs.next()) {
		    	  System.out.println(rs.getInt(1)+ " " + rs.getString(2)+ " " + rs.getInt(3));
		      }
		      rs.close();
		      final String result = ""
		          + "EMPNO=7369; ENAME=SMITH; JOB=CLERK; MGR=7902; HIREDATE=1980-12-17; SAL=800.00; COMM=null; DEPTNO=20\n"
		          + "EMPNO=7566; ENAME=JONES; JOB=MANAGER; MGR=7839; HIREDATE=1981-02-04; SAL=2975.00; COMM=null; DEPTNO=20\n"
		          + "EMPNO=7788; ENAME=SCOTT; JOB=ANALYST; MGR=7566; HIREDATE=1987-04-19; SAL=3000.00; COMM=null; DEPTNO=20\n"
		          + "EMPNO=7876; ENAME=ADAMS; JOB=CLERK; MGR=7788; HIREDATE=1987-05-23; SAL=1100.00; COMM=null; DEPTNO=20\n"
		          + "EMPNO=7902; ENAME=FORD; JOB=ANALYST; MGR=7566; HIREDATE=1981-12-03; SAL=3000.00; COMM=null; DEPTNO=20\n";
		    } catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		  }
	
		  // setting all up
		  
		  private String jsonPath(String model) {
			  return resourcePath(model + ".json");
		  }

		  private String resourcePath(String path) {
			  final URL url = RelAlgebraDemo.class.getResource("/resources/" + path);
			 
			  String s = url.toString();
			  if (s.startsWith("file:")) {
				  s = s.substring("file:".length());
			  }
			  return s;
		  }
		  
		  private FrameworkConfig  buildConfig() {
			  FrameworkConfig calciteFrameworkConfig= null;
			  
			  Connection connection = null;
			  Statement statement = null;
			  try {
				  Properties info = new Properties();
				  info.put("model", jsonPath("datamodel"));
			      connection = DriverManager.getConnection("jdbc:calcite:", info);
			      
			      final CalciteConnection calciteConnection = connection.unwrap(
			              CalciteConnection.class);

			      calConn = calciteConnection;
			      SchemaPlus rootSchemaPlus = calciteConnection.getRootSchema();
			      
			      final Schema schema =
			              CsvSchemaFactory.INSTANCE
			                  .create(rootSchemaPlus, null,
			                      ImmutableMap.<String, Object>of("directory",
			                          resourcePath("company"), "flavor", "scannable"));
			      

			      SchemaPlus companySchema = rootSchemaPlus.getSubSchema("company");
			    		  
			     // Set<String> tables= schema.getTableNames();
			     // for (String t: tables)
			     //	  System.out.println(t);
			      
			      System.out.println("Available tables in the database:");
			      Set<String>  tables=rootSchemaPlus.getSubSchema("company").getTableNames();
			      for (String t: tables)
			    	  System.out.println(t);
			      
			      
			      //final SchemaPlus rootSchema = Frameworks.createRootSchema(true);
			     
			      final List<RelTraitDef> traitDefs = new ArrayList<RelTraitDef>();

			      traitDefs.add(ConventionTraitDef.INSTANCE);
			      traitDefs.add(RelCollationTraitDef.INSTANCE);

			       calciteFrameworkConfig = Frameworks.newConfigBuilder()
			          .parserConfig(SqlParser.configBuilder()
			              // Lexical configuration defines how identifiers are quoted, whether they are converted to upper or lower
			              // case when they are read, and whether identifiers are matched case-sensitively.
			              .setLex(Lex.MYSQL)
			              .build())
			          // Sets the schema to use by the planner
			          .defaultSchema(companySchema) 
			          .traitDefs(traitDefs)
			          // Context provides a way to store data within the planner session that can be accessed in planner rules.
			          .context(Contexts.EMPTY_CONTEXT)
			          // Rule sets to use in transformation phases. Each transformation phase can use a different set of rules.
			          .ruleSets(RuleSets.ofList())
			          // Custom cost factory to use during optimization
			          .costFactory(null)
			          .typeSystem(RelDataTypeSystem.DEFAULT)
			          .build();
			     
			  } catch (Exception e) {
				  e.printStackTrace();
			  }
		      return calciteFrameworkConfig;
		  }
	
}
