package stocks;

import apidemo.ApiDemo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility that downloads a file from a URL.
 * @author www.codejava.net
 *
 */
public class HttpDownloadUtility {
	private static final int BUFFER_SIZE = 4096;

	/**
	 * Downloads a file from a URL
	 * @param fileURL HTTP URL of the file to be downloaded
	 * @param saveDir path of the directory to save the file
	 * @param fileName2 
	 * @throws IOException
	 */
	public static void downloadFile(String fileURL, String saveDir, String fileName)
			throws IOException {
		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			/*
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();
            ApiDemo.myPrintln("Content-Type = " + contentType);
            ApiDemo.myPrintln("Content-Disposition = " + disposition);
            ApiDemo.myPrintln("Content-Length = " + contentLength);
            ApiDemo.myPrintln("fileName = " + fileName);
			 */

			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();
			String saveFilePath = saveDir + File.separator + fileName;

			// opens an output stream to save into file
			FileOutputStream outputStream = new FileOutputStream(saveFilePath);

			int bytesRead = -1;
			byte[] buffer = new byte[BUFFER_SIZE];
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}

			outputStream.close();
			inputStream.close();

			ApiDemo.myPrintln(fileName + " downloaded");
		} else {
			ApiDemo.myPrintln("No file to download. Server replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
	}
}