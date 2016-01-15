package com.redmart.skiinginsingapore.util;

/**
 * Created by archie on 15/1/16.
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;

/**
 * Asynchronous http requests implementation.
 */
public class AsyncHttpURLConnection {
    private static final int HTTP_TIMEOUT_MS = 8000;
    private final String method;
    private final String url;
    private final String message;
    private final AsyncHttpEvents events;
    private String contentType;
    private boolean closed;

    /**
     * Http requests callbacks.
     */
    public interface AsyncHttpEvents {
        public void onHttpError(String errorMessage);
        public void onHttpComplete(String response);
    }

    public AsyncHttpURLConnection(String method, String url, String message,
                                  AsyncHttpEvents events) {
        this.method = method;
        this.url = url;
        this.message = message;
        this.events = events;
        closed = false;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void submit() {
        Runnable runHttp = new Runnable() {
            public void run() {
                sendHttpMessage();
            }
        };
        (new Thread(runHttp)).start();
    }

    private void sendHttpMessage() {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) new URL(url).openConnection();
            byte[] postData = new byte[0];
            if (message != null) {
                postData = message.getBytes("UTF-8");
            }
            connection.setRequestMethod(method);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            boolean doOutput = false;
            if (method.equals("POST")) {
                doOutput = true;
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(postData.length);
            }
            if (contentType == null) {
                connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            } else {
                connection.setRequestProperty("Content-Type", contentType);
            }

            // Send POST request.
            if (doOutput && postData.length > 0) {
                OutputStream outStream = connection.getOutputStream();
                outStream.write(postData);
                outStream.close();
            }

            // Get response.
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                if(!closed)
                    events.onHttpError("Non-200 response to " + method + " to URL: "
                        + url + " : " + connection.getHeaderField(null));
                connection.disconnect();
                return;
            }
            InputStream responseStream = connection.getInputStream();
            String response = drainStream(responseStream);
            responseStream.close();
            connection.disconnect();
            if(!closed)
                events.onHttpComplete(response);
        } catch (SocketTimeoutException e) {
            if(!closed)
                events.onHttpError("HTTP " + method + " to " + url + " timeout");
        } catch (IOException e) {
            if(!closed)
                events.onHttpError("HTTP " + method + " to " + url + " error: "
                    + e.getMessage());
        }
    }

    public void close() {
        closed = true;
    }

    // Return the contents of an InputStream as a String.
    private static String drainStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
