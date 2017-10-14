package com.spotonresponse.xchangecore;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class OpenFeedProcessor {

    public static final int timeout = 30;


    private static final Class thisClass = OpenFeedProcessor.class;
    private static final Logger logger = LogManager.getLogger(thisClass);
    private static final String properties_filename = "openfeedprocessor.properties";
    private static Properties prop = new Properties();
    private static int exitStatus = 0;

    private static int DEFAULT_POLL_INTERVAL = 3600;   // 3600 seconds = 1 hour

    // Class object to store the parameters
    static class AuthInfo {
        String url;
        String username;
        String password;
        int pollInterval;
        Path outputFile;
    }


    public static void processURLs() {
        Boolean moreURLs = true;
        int urlcount = 1;


        int maxThreads = 10;

        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                maxThreads,
                maxThreads,
                10L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(maxThreads, true),
                new ThreadPoolExecutor.CallerRunsPolicy());


        while (moreURLs) {
            try {
                // This variable will be used during sanity check of the properties
                Boolean proceed = false;

                // Load the properties into a object we can continue to reference
                AuthInfo authInfo = new AuthInfo();

                // We are looping through the properties file looking for
                // numbered entries.  As soon as what we are looking for does
                // not exist we stop looking.  Not the best method, but...
                if (prop.getProperty("url" + urlcount) == null) {
                    moreURLs = false;
                    break;
                }
                authInfo.url = prop.getProperty("url" + urlcount);
                if (!authInfo.url.isEmpty()) {
                    proceed = true;
                }

                if (prop.getProperty("pollInterval" + urlcount) == null) {
                    authInfo.pollInterval = DEFAULT_POLL_INTERVAL;
                } else {
                    authInfo.pollInterval = Integer.valueOf(prop.getProperty("pollInterval" + urlcount));
                }

                // Grab the username and password properties and validate them
                authInfo.username = prop.getProperty("username" + urlcount);
                authInfo.password = prop.getProperty("password" + urlcount);

                // If the default username is still in the properties file, let the user know the
                // file is not configured
                if (authInfo.username.equals("username")) {
                    logger.error("You must configure the properties file: " + properties_filename);
                    proceed = false;
                }

                // If the username or password is empty - we cannot use the URL
                if (authInfo.username.isEmpty()) {
                    logger.error("Must specify a valid username in the properties file");
                    proceed = false;
                }
                if (authInfo.password.isEmpty()) {
                    logger.error("Must specify a valid password in the properties file");
                    proceed = false;
                }


                // Check the property for the output file
                if (prop.getProperty("outfile" + urlcount) != null) {
                    authInfo.outputFile = Paths.get(prop.getProperty("outfile" + urlcount));
                    logger.info("Going to save to file: " + authInfo.outputFile.toString());
                } else {
                    logger.error("No output file specified for URL: " + authInfo.url);
                    proceed = false;
                }



                // If all is well, log the information we have and fetch the data
                if (proceed) {
                    logger.info("URL: " + authInfo.url);
                    logger.debug("Using username: " + authInfo.username);

                    tpe.execute(new PollXCore(authInfo));

                } else {
                    logger.error("Problem with properties file");
                }

            } catch (Exception ex) {
                logger.error(ex);
            }

            // See if we have more URLs in the properties file
            urlcount++;
        }
    }



    // If there is no properties, this method will create one
    private static void createPropertiesFile() {
        OutputStream output = null;

        try {
            logger.info("Creating properties file: " + properties_filename);
            Properties prop = new Properties();
            output = new FileOutputStream(properties_filename);

            // set the properties value
            prop.setProperty("url1", "{see comments above}");
            prop.setProperty("username1", "username");
            prop.setProperty("password1", "password");
            prop.setProperty("outfile1", "outfile1");

            // save properties to project root folder
            prop.store(output, "Properties file for the XchangeCore OpenFeedProcessor. Specify the Full URL like this: https://host.domain.com/xchangecore/pub/search?full=true&productType=Incident&productType=Alert&productType=SOI&productType=MapViewContext&format=xml");

            logger.info("Properties file: " + properties_filename + " ready for customization");
        } catch (IOException io) {
            io.printStackTrace();
            logger.error(io);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e);
                }
            }

        }
    }


    private static boolean loadProperties() {
        InputStream input = null;

        logger.debug("Reading properties file");
        // load a properties file, if it does not exist, create one and then exit
        try {
            input = new FileInputStream(properties_filename);
            prop.load(input);
        } catch (FileNotFoundException fnf) {
            // Most likely the initial run of this script
            // Create a generic properties file for the user and then exit
            createPropertiesFile();
            exitStatus = 1;
            return false;
        } catch (IOException ex) {
            logger.fatal("Unable to read existing properties file");
            exitStatus = 2;
            return false;
        } finally {
            if (input != null) {
                try {
                    input.close();
                    logger.debug("Closing input stream");
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error(e);
                }
            }
        }
        logger.info("Properties file read");
        return true;
    }


    public static void main(String[] args) {

        String version = thisClass.getPackage().getImplementationVersion();
        logger.info(thisClass.getSimpleName() + " Version: " + version + " starting up.");

        // Load the properties file
        if (loadProperties()) {
            // If successful, process the URLs
            processURLs();
        } else {
            logger.fatal("Unable to continue");
            System.exit(exitStatus);
        }

    }

}