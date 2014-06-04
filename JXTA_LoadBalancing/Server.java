/**
 * Server side: This is the server side  */

import java.io.*;
//import java.io.StringWriter;

import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.StructuredDocumentUtils;
import net.jxta.document.StructuredTextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.PipeService;
import net.jxta.platform.ModuleClassID;
import net.jxta.protocol.ModuleClassAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
/* new added */
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.util.JxtaServerPipe;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.impl.protocol.*;
import net.jxta.impl.resolver.*;
import net.jxta.resolver.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.jxta.resolver.QueryHandler;
import net.jxta.protocol.ResolverQueryMsg;
import net.jxta.protocol.ResolverResponseMsg;
import java.io.IOException;


public class Server   {

    static PeerGroup group = null;
    static PeerGroupAdvertisement groupAdvertisement = null;
    private DiscoveryService discovery;
    private PipeService pipes;
    private JxtaServerPipe serverPipe;//server pipe
    private InputPipe myPipe; // input pipe for the service
    private Message msg;      // pipe message received
    private ID gid;  // group id
    private ResolverServiceImpl resolver;
    static PipeAdvertisement pipeadv = null, pipeAdv = null;
    public static final int ITERATIONS = 100;
    protected SimpleDateFormat format   = new SimpleDateFormat("MM, dd, yyyy hh:mm:ss.S");
    protected StructuredTextDocument credential = (StructuredTextDocument) StructuredDocumentFactory.newStructuredDocument( new MimeMediaType("text", "xml"),"Server");
    protected String handlerName = "MicroQueryHandler";
    private int numberHops = -1; //default hop
    private long timeDelta = 0; //time difference
    private long timeNow;  //current time
    private boolean runMicroTest = false;
    private static final int DEFAULT_LIFETIME = 60 * 1000;


    public static void main(String args[]) {
        Server myapp = new Server();
        System.out.println ("Starting Service Peer ....");
        myapp.startJxta();
	myapp.startServer();
        System.out.println ("Good Bye ....");
        System.exit(0);
    }

    private void startJxta() {
        try {
            // create, and Start the default jxta NetPeerGroup
            group = PeerGroupFactory.newNetPeerGroup();

        } catch (PeerGroupException e) {
            // could not instantiate the group, print the stack and exit
            System.out.println("fatal error : group creation failure");
            e.printStackTrace();
            System.exit(1);
        }
        // this is how to obtain the group advertisement
       groupAdvertisement = group.getPeerGroupAdvertisement();

        // get the discovery, and pipe service
        System.out.println("Getting DiscoveryService");
        discovery = group.getDiscoveryService();
        System.out.println("Getting PipeService");
        pipes = group.getPipeService();
	microTest();
    }

    private void startServer() {

        System.out.println("Start the Server daemon");
        // get the peergroup service we need
        gid = group.getPeerGroupID();

        try {

	    // create the Module class advertisement associated with the service

            ModuleClassAdvertisement mcadv = (ModuleClassAdvertisement)
                                             AdvertisementFactory.newAdvertisement(
                                                 ModuleClassAdvertisement.getAdvertisementType());

            mcadv.setName("JXTAMOD:JXTA-FileMain");
            mcadv.setDescription("Contains FileMain.txt advertisement");

            ModuleClassID mcID = IDFactory.newModuleClassID();
            mcadv.setModuleClassID(mcID);

            // publish

            //discovery.publish(mcadv);
            discovery.remotePublish(mcadv,DEFAULT_LIFETIME );

            // Create the Module Spec advertisement associated with the service

            ModuleSpecAdvertisement mdadv = (ModuleSpecAdvertisement)
                                            AdvertisementFactory.newAdvertisement(
                                                ModuleSpecAdvertisement.getAdvertisementType());

            // Setup some of the information field about the servive. In this

            mdadv.setName("JXTASPEC:JXTA-FileMain");
            mdadv.setVersion("Version 3.0");
            mdadv.setCreator("Sarang's Server1");
            mdadv.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
            mdadv.setSpecURI("http://www.jxta.org/FileA");
	    String serverID = group.getPeerID().toString ();
 	    String name = group.getPeerName ();
	    System.out.println ( "My ID: " + serverID + ", Name: " + name );
            mdadv.setDescription ( serverID ); //sending client to server time difference

            // Create a pipe advertisement for the Service

            System.out.println("Reading in pipeserver.adv");
            //PipeAdvertisement pipeadv = null;

            try {
                FileInputStream is = new FileInputStream("pipeserver.adv");
                pipeadv = (PipeAdvertisement)
                          AdvertisementFactory.newAdvertisement(
                              MimeMediaType.XMLUTF8, is);
                is.close();
            } catch (Exception e) {
                System.out.println("failed to read/parse pipe advertisement");
                e.printStackTrace();
                System.exit(-1);
            }

	    pipeadv.setDescription(""+numberHops+","+timeDelta+","+timeNow); //sending number of hops and time
	    // Store the pipe advertisement in the spec adv.

            mdadv.setPipeAdvertisement(pipeadv);

            // display the advertisement as a plain text document.
            StructuredTextDocument doc = (StructuredTextDocument)
                                         mdadv.getDocument(MimeMediaType.TEXT_DEFAULTENCODING);

            StringWriter out = new StringWriter();
            doc.sendToWriter(out);
            System.out.println(out.toString());
            out.close();


            // Ok the Module advertisement was created, just publish
            // it in my local cache and into the NetPeerGroup.
            //discovery.publish(mdadv);
            discovery.remotePublish(mdadv,DEFAULT_LIFETIME);
	    // create the input pipe endpoint clients will
            // use to connect to the service
            myPipe = pipes.createInputPipe(pipeadv);
	    discovery.flushAdvertisement(pipeadv);

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Server: Error publishing the module");
	}

        while (true) { // loop over every input received from clients

            System.out.println("Waiting for client messages to arrive");

            try {

                // Listen on the pipe for a client message
                msg = myPipe.waitForMessage();

            } catch (Exception e) {
                myPipe.close();
                System.out.println("Server: Error listening for message");
                return;
            }

            // Read the message as a String
            String ip = null;

            try {

                // get all the message elements
                Message.ElementIterator en = msg.getMessageElements();
                if ( !en.hasNext() ) {
                    return;
                }
                // get the message element named SenderMessage
                MessageElement msgElement = msg.getMessageElement(null, "DataTag");
                // Get message
                if (msgElement.toString() != null) {
                    ip = msgElement.toString();
                }

                if (ip != null) {
                    // read the data
                    System.out.println("Server: receive message: " + ip);
		    createBiDiPipe();
		    sendFile();
                } else {
                    System.out.println("Server: error could not find the tag");
                }

            } catch (Exception e) {
                System.out.println("Server: error receiving message");
            }

        }

    }

    //Create bidirectional pipe to send the file
    private void createBiDiPipe(){

	try {
		FileInputStream is = new FileInputStream("pipe.adv");
            	pipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
            	is.close();
		serverPipe = new JxtaServerPipe(group, pipeAdv);
            	// we want to block until a connection is established
            	serverPipe.setPipeTimeout(0);
	    } catch (Exception e) {
            	System.out.println("failed to bind to the JxtaServerPipe due to the following exception");
            	e.printStackTrace();
            	System.exit(-1);
        }

    }

    //read the file
    private void sendTestMessages(JxtaBiDiPipe pipe) {
        try {
		BufferedReader input = null;
		input = new BufferedReader( new FileReader("FileA.txt") );
      		String line = null; //not declared within while loop
		while (( line = input.readLine()) != null){
			Message fileMsg = new Message();
            		fileMsg.addMessageElement(new StringMessageElement("ServerMsg",line,null));
            		System.out.println("Sending :"+line);
            		pipe.sendMessage(fileMsg);
      		}
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    //mechainsm to send the file
    public void sendFile() {

        System.out.println("Waiting for JxtaBidiPipe connections on JxtaServerPipe");
        while (true) {
            try {
                JxtaBiDiPipe bipipe = serverPipe.accept();
                if (bipipe != null ) {
                    System.out.println("JxtaBidiPipe accepted, sending server FileA.txt to the client");
                    sendTestMessages(bipipe);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    //============ Micro test code --- uses resolve query services =================

    private void microTest(){
    	ResolverService resolver = group.getResolverService();
        MicroQueryHandler handler = new  MicroQueryHandler(handlerName,credential);
        resolver.registerHandler(handlerName, handler);

	System.out.println("Waiting for Query message from Client");

	while (runMicroTest == false);
	// Now it is true.
	runMicroTest = false;
    }

    class MicroQueryHandler implements QueryHandler{
        protected String handlerName;
        protected StructuredTextDocument credential;
        protected SimpleDateFormat format   = new SimpleDateFormat("MM, dd, yyyy hh:mm:ss.S");

	//constructor
        public MicroQueryHandler(String handlerName, StructuredTextDocument credential){
            this.handlerName = handlerName;
            this.credential = credential;
        }

        public void processResponse(ResolverResponseMsg response) {
            System.out.println("Received a response");
        }

        public int processQuery(ResolverQueryMsg query) {
            System.out.println("Received a query");
            System.out.println(((ResolverQuery)query).toString());

            StructuredTextDocument doc = null;
            String textDoc = query.getQuery();
            try{
                doc = (StructuredTextDocument)StructuredDocumentFactory.newStructuredDocument(new MimeMediaType( "text/xml"), new ByteArrayInputStream(textDoc.getBytes()) );
            }catch(java.io.IOException ioe){
                System.err.println("Unable to decode XML for query");
                ioe.printStackTrace();
            }

            timeNow = System.currentTimeMillis(); //current time
	    String timePrev = doc.getValue().toString(); //sent by client
	    long timePast = Long.parseLong(timePrev);
	    timeDelta = timeNow-timePast;
	    System.out.println("Received Time: " + format.format( new Date(timeNow)));
	    numberHops = query.getHopCount();
	    System.out.println("No. of hops: " + numberHops + "; Initiated Time: " +  timePast + "; Query Time: " + timeDelta);
	    runMicroTest = true;
            return ResolverService.OK;
        }
    }
    //============ End - Micro test code  =================
}

