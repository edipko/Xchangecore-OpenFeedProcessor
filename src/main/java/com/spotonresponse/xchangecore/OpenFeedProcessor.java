package com.spotonresponse.xchangecore;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;


public class OpenFeedProcessor {

    private static final Class thisClass = OpenFeedProcessor.class;


    private static final Logger logger = LogManager.getLogger(thisClass);
    private static final int timeout = 30;
    private static final String properties_filename = "feedpuller.properties";


    // Class object to store the parameters
    static class AuthInfo {
        String url;
        String feedtype;
        String username;
        String password;
    }


    public static void main(String[] args) {
        Properties prop = new Properties();
        InputStream input = null;

        try {

            String version = thisClass.getPackage().getImplementationVersion();
            logger.info(thisClass.getSimpleName() + " Version: " + version + " starting up.");

            // load a properties file, if it does not exist, create one and then exit
            try {
                input = new FileInputStream(properties_filename);
                prop.load(input);
            } catch (FileNotFoundException fnf) {
                // Most likely the initial run of this script
                // Create a generic properties file for the user and then exit
                createPropertiesFile();
                System.exit(1);
            }


            // This variable will be used during sanity check of the properties
            Boolean proceed = false;

            // Load the properties into a object we can continue to reference
            AuthInfo authInfo = new AuthInfo();

            // Sanity check the protocol property
            String protocol = prop.getProperty("protocol");
            String useProtocol = "https://";
            if ((protocol.equals("http")) || (protocol.equals("https"))) {
                useProtocol = protocol + "://";
            } else {
                logger.warn("Bad protocol given, going to try https");
            }

            // Make sure the use did not include the protocol in the url property
            authInfo.url = useProtocol + prop.getProperty("url")
                    .replace("https://", "")
                    .replace("http://", "");

            // Grab the feed type property and verify it is acceptable
            authInfo.feedtype = prop.getProperty("feedtype").toLowerCase();
            if (authInfo.feedtype.equals("rss") ||
                    authInfo.feedtype.equals("kml") ||
                    authInfo.feedtype.equals("xml")) {
                proceed = true;
            } else {
                logger.error("Bad feedtype property: " + authInfo.feedtype);
            }


            // Grab the username and password properties and validate them
            authInfo.username = prop.getProperty("username");
            authInfo.password = prop.getProperty("password");
            if (authInfo.username.equals("username")) {
                logger.error("You must configure the properties file: " + properties_filename);
                proceed = false;
            }
            if (authInfo.username.isEmpty()) {
                logger.error("Must specify a valid username in the properties file");
                proceed = false;
            }
            if (authInfo.password.isEmpty()) {
                logger.error("Must specify a valid password in the properties file");
                proceed = false;
            }


            // If all is well, log the information we have and fetch the data
            if (proceed) {
                logger.debug("Propertied file read");
                logger.info("URL: " + authInfo.url);
                logger.info("Requesting feed type: " + authInfo.feedtype);
                logger.debug("Using username: " + authInfo.username);

                Path outFile = Paths.get(authInfo.username + "_output." + authInfo.feedtype);

                // Get the stream
                InputStream data = getData(authInfo);
                if (data != null) {
                    // Write the file
                    Files.copy(data, outFile, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Output saved to file: " + outFile.toString());
                } else {
                    logger.error("Data stream was empty");
                }
            } else {
                logger.error("Problem with properties file");
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error(ex);

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
    }


    // This class will actually pull the data from XChangeCore
    private static InputStream getData(AuthInfo authInfo) {
        InputStream result = null;
        HttpURLConnection urlConnection = null;
        try {
            String authString = authInfo.username + ":" + authInfo.password;
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);

            URL url = new URL(authInfo.url + "/xchangecore/pub/search?full=true&productType=Incident&productType=Alert&productType=SOI&productType=MapViewContext&format=" + authInfo.feedtype);
            logger.info("Connecting to UICDS: " + url.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(timeout * 1000);
            urlConnection.setRequestProperty("Authorization", "Basic "
                    + authStringEnc);
            result = urlConnection.getInputStream();

        } catch (SocketTimeoutException ste) {
            logger.error("Unable to connect to Xchangecore");
        } catch (IOException ioex) {
            if (urlConnection.getResponseCode() == 401) {
                logger.error("Authentication Fails - check username and password");
            }
        } catch (Exception ex) {
            logger.error("Error getting XChangecore data" + ex);
            ex.printStackTrace();
        } finally {
            return result;
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
            prop.setProperty("protocol", "https");
            prop.setProperty("url", "host.domain.com");
            prop.setProperty("feedtype", "one of  xml, kml, or rss");
            prop.setProperty("username", "username");
            prop.setProperty("password", "password");

            // save properties to project root folder
            prop.store(output, "Properties file for the XchangeCore FeedPuller Adapter");

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

}