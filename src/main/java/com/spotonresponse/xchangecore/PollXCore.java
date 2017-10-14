package com.spotonresponse.xchangecore;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PollXCore implements Runnable {

    private static final Class thisClass = PollXCore.class;
    private static final Logger logger = LogManager.getLogger(thisClass);
    private OpenFeedProcessor.AuthInfo authInfo;

    public PollXCore(OpenFeedProcessor.AuthInfo _authInfo) {
        this.authInfo = _authInfo;
    }

    public void run() {

        try {

            // Continously loop, sleeping for the specified poll interval between each run
            while (true) {
                // Get the stream
                InputStream data = getData(authInfo);
                if (data != null) {
                    // Write the file
                    Files.copy(data, authInfo.outputFile, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Output saved to file: " + authInfo.outputFile.toString());
                } else {
                    logger.error("Data stream was empty");
                }

                Thread.sleep(authInfo.pollInterval * 1000);
            }


        } catch (IOException ex) {
            ex.printStackTrace();
            logger.error(ex);

        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }

    // This class will actually pull the data from XChangeCore
    private static InputStream getData(OpenFeedProcessor.AuthInfo authInfo) {
        InputStream result = null;
        HttpURLConnection urlConnection = null;
        try {
            String authString = authInfo.username + ":" + authInfo.password;
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);

            URL url = new URL(authInfo.url);
            logger.info("Connecting to UICDS: " + url.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(OpenFeedProcessor.timeout * 1000);
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
}
