/**
 * Writer.java takes a data structure representation of an XML graph
 * (CFG, ICFG, or CHG) and converts it to table in HTML format.
 *
**/

import java.io.*;
import java.lang.*;
import java.util.Vector;

public class Writer
{
	static FileOutputStream		fileOutputStream;
	static OutputStreamWriter	outputStreamWriter;
	static PrintWriter		printWriter;


	public Writer() {

	}


	public static void main (String argv[])
	{
		System.out.println( "ERROR: This is not a runable class" );
		System.exit(0);

	} // end main()


	public static void runWriter (Vector vecGraphs, String outputFilename, boolean simpleDisplay)
	{
		prepareWrite(outputFilename);
		writeHeader( vecGraphs, !(simpleDisplay) );
		writeBody( vecGraphs, !(simpleDisplay) );
		writeFooter();

		try
		{
			printWriter.close();
			outputStreamWriter.close();
			fileOutputStream.close();
		} // end try

		catch ( Exception e )
		{
			System.out.println( "ERROR: Could not close output streams" );
		} // end catch

	} // end main()


	public static void prepareWrite( String outputFilename )
	{
		try
		{
			fileOutputStream = new FileOutputStream( outputFilename );
			outputStreamWriter = new OutputStreamWriter( fileOutputStream );
			printWriter = new PrintWriter( outputStreamWriter );
		} // end try

		catch ( Exception e )
		{
			System.out.println( "ERROR: Could not create output file" );
		} // end catch

	} // end prepareWrite()


	public static void writeHeader( Vector vecGraphs, boolean detailDisplay )
	{
		Graph graph = (Graph)vecGraphs.elementAt( vecGraphs.size() - 1 );
		String type = graph.GetType();
		String detail = "simple view";

		if( detailDisplay )
			detail = "detail view";

		printWriter.println( "<html>" );
		printWriter.println( "\t<head>" );
		printWriter.println( "\t\t<title>CS6300: Jaba Graph</title>" );
		printWriter.println( "\t</head>\n" );
		printWriter.println( "\t<body>" );
		printWriter.println( "\t<h1>" + type + ": " + detail + "</h1>" );
		printWriter.println( "\t\t<table border=1>" );
		printWriter.println( "\t\t\t<tr>" );

		if( type.equals("CHG") )
			printWriter.println( "\t\t\t\t<th>Name</th>" );

		printWriter.println( "\t\t\t\t<th>Node ID</th>" );
		printWriter.println( "\t\t\t\t<th>Node Type</th>" );

		if( type.equals("CHG") )
			printWriter.println( "\t\t\t\t<th>Type ID</th>" );
		else
		{
			printWriter.println( "\t\t\t\t<th>Successors</th>" );
			printWriter.println( "\t\t\t\t<th>Source Line</th>" );
			printWriter.println( "\t\t\t\t<th>Bytecode Offset</th>" );

			if( detailDisplay )
			{
				printWriter.println( "\t\t\t\t<th>Node Attributes</th>" );
				printWriter.println( "\t\t\t\t<th>Edge Attributes</th>" );
			}
		}

		printWriter.println( "\t\t\t</tr>" );

	} // end writeHeader()

	
	public static void writeBody( Vector vecGraphs, boolean detailDisplay )
	{
		Node node;
		String type;				// the type of graph

		while( !(vecGraphs.isEmpty()) )
		{
			/* get a vector or nodes */
			Graph graph = (Graph)vecGraphs.remove(0);

			type = graph.GetType();
	
			while( graph.MoreNodes() )
			{

				node = graph.GetNode();
	
				printWriter.println( "\t\t\t<tr>" );
		
				if( type.equals("CHG") )
					printWriter.println( "\t\t\t\t<td>" + node.GetName() + "</td>" );

				printWriter.println( "\t\t\t\t<td>" + node.GetNodeID() + "</td>" );
				printWriter.println( "\t\t\t\t<td>" + node.GetNodeType() + "</td>" );
				
				if( type.equals("CHG") )
					printWriter.println( "\t\t\t\t<td>" + node.GetTypeID() + "</td>" );
				else
					{
					printWriter.println( "\t\t\t\t<td>" + node.GetSuccessors() + "</td>" );
					printWriter.println( "\t\t\t\t<td>" + node.GetSourceLine() + "</td>" );
					printWriter.println( "\t\t\t\t<td>" + node.GetBytecodeOffset() + "</td>" );

					if( detailDisplay )
					{
						printWriter.println( "\t\t\t\t<td>" + node.GetNodeAttributes() + "</td>" );
						printWriter.println( "\t\t\t\t<td>" + node.GetEdgeAttributes() + "</td>" );
					}
				}

				printWriter.println( "\t\t\t</tr>\n" );
	
			} // end while
		} // end while
	} // writeBody()


	public static void writeFooter()
	{
		printWriter.println( "\t\t</table>" );
		printWriter.println( "\t</body>" );
		printWriter.println( "</html>" );

	} // end writeFooter()


} // end Writer
