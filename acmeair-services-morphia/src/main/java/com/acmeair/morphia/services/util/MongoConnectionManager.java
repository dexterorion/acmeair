package com.acmeair.morphia.services.util;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

// Resource injection removed - using direct instantiation
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.acmeair.morphia.BigDecimalConverter;
import com.acmeair.morphia.MorphiaConstants;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

public class MongoConnectionManager implements MorphiaConstants{

	private static AtomicReference<MongoConnectionManager> connectionManager = new AtomicReference<MongoConnectionManager>();
	
	private final static Logger logger = Logger.getLogger(MongoConnectionManager.class.getName());
	
	protected DB db;
	private static Datastore datastore;
	
	public static MongoConnectionManager getConnectionManager() {
		if (connectionManager.get() == null) {
			synchronized (connectionManager) {
				if (connectionManager.get() == null) {
					connectionManager.set(new MongoConnectionManager());
				}
			}
		}
		return connectionManager.get();
	}
	
	
	private MongoConnectionManager (){

		Morphia morphia = new Morphia();
		// Set default client options, and then check if there is a properties file.
		boolean fsync = false;
		int w = 0;
		int connectionsPerHost = 5;
		int threadsAllowedToBlockForConnectionMultiplier = 10;
		int connectTimeout= 30000;  // 30 seconds timeout instead of 0
		int socketTimeout= 30000;   // 30 seconds socket timeout
		boolean socketKeepAlive = true;
		int maxWaitTime = 10000;    // 10 seconds max wait time


		Properties prop = new Properties();
		URL mongoPropertyFile = MongoConnectionManager.class.getResource("/com/acmeair/morphia/services/util/mongo.properties");

		if(mongoPropertyFile != null){
			try {
				logger.info("Reading mongo.properties file");
				prop.load(mongoPropertyFile.openStream());
				fsync = Boolean.valueOf(prop.getProperty("mongo.fsync"));
				w = Integer.valueOf(prop.getProperty("mongo.w"));
				connectionsPerHost = Integer.valueOf(prop.getProperty("mongo.connectionsPerHost"));
				threadsAllowedToBlockForConnectionMultiplier = Integer.valueOf(prop.getProperty("mongo.threadsAllowedToBlockForConnectionMultiplier"));
				connectTimeout= Integer.valueOf(prop.getProperty("mongo.connectTimeout"));
				socketTimeout= Integer.valueOf(prop.getProperty("mongo.socketTimeout"));
				socketKeepAlive = Boolean.valueOf(prop.getProperty("mongo.socketKeepAlive"));
				maxWaitTime =Integer.valueOf(prop.getProperty("mongo.maxWaitTime"));
			}catch (IOException ioe){
				logger.severe("Exception when trying to read from the mongo.properties file" + ioe.getMessage());
			}
		}
		
		// Set the client options
		MongoClientOptions.Builder builder = new MongoClientOptions.Builder()
			.writeConcern(new WriteConcern(w, 0, fsync))
			.connectionsPerHost(connectionsPerHost)
			.connectTimeout(connectTimeout)
			.socketTimeout(socketTimeout)
			.socketKeepAlive(socketKeepAlive)
			.maxWaitTime(maxWaitTime)
			.threadsAllowedToBlockForConnectionMultiplier(threadsAllowedToBlockForConnectionMultiplier);

				
		try {
			//Check if VCAP_SERVICES exist, and if it does, look up the url from the credentials.
			String vcapJSONString = System.getenv("VCAP_SERVICES");
			if (vcapJSONString != null) {
				logger.info("Reading VCAP_SERVICES");
				Object jsonObject = JSONValue.parse(vcapJSONString);
				JSONObject vcapServices = (JSONObject)jsonObject;
				JSONArray mongoServiceArray =null;					
				for (Object key : vcapServices.keySet()){
					if (key.toString().startsWith("mongo")){
						mongoServiceArray = (JSONArray) vcapServices.get(key);
						break;
					}
				}
				
				if (mongoServiceArray == null) {
					logger.severe("VCAP_SERVICES existed, but a mongo service was not definied.");
				} else {					
					JSONObject mongoService = (JSONObject)mongoServiceArray.get(0); 
					JSONObject credentials = (JSONObject)mongoService.get("credentials");
					String url = (String) credentials.get("url");
					logger.fine("service url = " + url);				
					MongoClientURI mongoURI = new MongoClientURI(url, builder);
					MongoClient mongo = new MongoClient(mongoURI);

					morphia.getMapper().getConverters().addConverter(new BigDecimalConverter());
					datastore = morphia.createDatastore( mongo ,mongoURI.getDatabase());
				}	

			} else {
				//VCAP_SERVICES don't exist, so use the DB resource  
				logger.fine("No VCAP_SERVICES found");
				if(db == null){
					try {
						logger.warning("Resource Injection failed. Attempting to look up " + JNDI_NAME + " via JNDI.");
						db = (DB) new InitialContext().lookup(JNDI_NAME);
					} catch (NamingException e) {
						logger.severe("Caught NamingException : " + e.getMessage() );
					}	        
				}

				if(db == null){
					String host = "localhost";
					String port = "27017";
					String database = "acmeair";

					logger.info("Creating the MongoDB Client connection. Looking up host and port information" );
					try {
						// Try JNDI first, fallback to defaults
						try {
							host = (String) new InitialContext().lookup("java:comp/env/" + HOSTNAME);
							port = (String) new InitialContext().lookup("java:comp/env/" + PORT);
							database = (String) new InitialContext().lookup("java:comp/env/" + DATABASE);
							logger.info("Using JNDI configuration: " + host + ":" + port + "/" + database);
						} catch (NamingException ne) {
							logger.info("JNDI lookup failed, using default MongoDB connection: " + host + ":" + port + "/" + database);
						}

						ServerAddress server = new ServerAddress(host, Integer.parseInt(port));
						MongoClient mongo = new MongoClient(server, builder.build());

						// Test the connection
						try {
							mongo.getAddress(); // This will throw if can't connect
							db = mongo.getDB(database);
							logger.info("Successfully connected to MongoDB at " + host + ":" + port + "/" + database);
						} catch (Exception connEx) {
							logger.warning("Failed to connect to MongoDB at " + host + ":" + port + " - " + connEx.getMessage());
							mongo.close();
							throw connEx;
						}
					} catch (Exception e) {
						logger.severe("Failed to connect to MongoDB: " + e.getMessage() );
						e.printStackTrace();
					}
				}

				if(db == null){
					logger.severe("Unable to retrieve reference to database, please check the server logs.");
					logger.severe("Make sure MongoDB is running on localhost:27017 or configure proper connection details.");
				} else {
					try {
						morphia.getMapper().getConverters().addConverter(new BigDecimalConverter());
						// Create datastore directly from the existing MongoDB connection
						// Cast Mongo to MongoClient (they are compatible in this version)
						MongoClient mongoClient = (MongoClient) db.getMongo();
						datastore = morphia.createDatastore(mongoClient, db.getName());
						logger.info("Morphia datastore created successfully");
					} catch (Exception e) {
						logger.severe("Failed to create Morphia datastore: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			logger.severe("Caught Exception : " + e.getMessage() );
		}

		logger.info("created mongo datastore with options:"+datastore.getMongo().getMongoClientOptions());
	}
	
	public DB getDB(){
		return db;
	}
	
	public Datastore getDatastore(){
		return datastore;
	}
	
	@SuppressWarnings("deprecation")
	public String getDriverVersion(){
		// MongoDB driver version is not directly available in newer drivers
		return "3.12.14";
	}
	
	public String getMongoVersion(){
		return datastore.getDB().command("buildInfo").getString("version");
	}
}
