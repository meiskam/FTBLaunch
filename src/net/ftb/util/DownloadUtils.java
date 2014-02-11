/*
 * This file is part of FTB Launcher.
 *
 * Copyright © 2012-2013, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/>
 * FTB Launcher is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file was edited by meiskam <meiskam@gmail.com>
 */
package net.ftb.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Scanner;

import net.ftb.data.Settings;
import net.ftb.gui.LaunchFrame;
import net.ftb.gui.dialogs.AdvancedOptionsDialog;
import net.ftb.log.Logger;

public class DownloadUtils extends Thread {
	public static boolean serversLoaded = false; 
	public static HashMap<String, String> downloadServers = new HashMap<String, String>();
	public static HashMap<String, String> backupServers = new HashMap<String, String>();

	/**
	 * @param file - the name of the file, as saved to the repo (including extension)
	 * @return - the direct link
	 * @throws NoSuchAlgorithmException - see md5
	 */
	public static String getCreeperhostLink(String file) throws NoSuchAlgorithmException {
		String resolved = (downloadServers.containsKey(Settings.getSettings().getDownloadServer())) ? "http://" + downloadServers.get(Settings.getSettings().getDownloadServer()) : "http://repo.feed-the-dojo.incraftion.com";
		resolved += "/FTB2/" + file;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(resolved).openConnection();
			for(String server : downloadServers.values()) {
				if(connection.getResponseCode() != 200) {
					resolved = "http://" + server + "/FTB2/" + file;
					connection = (HttpURLConnection) new URL(resolved).openConnection();
				} else {
					break;
				}
			}
		} catch (IOException e) { }
		connection.disconnect();
		return resolved;
	}

	/**
	 * @param file - the name of the file, as saved to the repo (including extension)
	 * @return - the direct link
	 */
	public static String getStaticCreeperhostLink(String file) {
		String resolved = (downloadServers.containsKey(Settings.getSettings().getDownloadServer())) ? "http://" + downloadServers.get(Settings.getSettings().getDownloadServer()) : "http://repo.feed-the-dojo.incraftion.com";
		resolved += "/FTB2/static/" + file;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(resolved).openConnection();
			if(connection.getResponseCode() != 200) {
				for(String server : downloadServers.values()) {
					if(connection.getResponseCode() != 200) {
						resolved = "http://" + server + "/FTB2/static/" + file;
						connection = (HttpURLConnection) new URL(resolved).openConnection();
					} else {
						break;
					}
				}
			}
		} catch (IOException e) { }
		connection.disconnect();
		return resolved;
	}

	/**
	 * @param file - file on the repo in static
	 * @return boolean representing if the file exists 
	 */
	public static boolean staticFileExists(String file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(getStaticCreeperhostLink(file)).openStream()));
			return !reader.readLine().toLowerCase().contains("not found");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @param file - file on the repo
	 * @return boolean representing if the file exists 
	 */
	public static boolean fileExists(String file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new URL("http://repo.feed-the-dojo.incraftion.com/FTB2/" + file).openStream()));
			return !reader.readLine().toLowerCase().contains("not found");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Downloads data from the given URL and saves it to the given file
	 * @param filename - String of destination
	 * @param urlString - http location of file to download
	 */
	public static void downloadToFile(String filename, String urlString) throws IOException {
		downloadToFile(new URL(urlString), new File(filename));
	}

	/**
	 * Downloads data from the given URL and saves it to the given file
	 * @param url The url to download from
	 * @param file The file to save to.
	 */
	public static void downloadToFile(URL url, File file) throws IOException {
	    file.getParentFile().mkdirs();
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		fos.close();
	}

	/**
	 * Checks the file for corruption.
	 * @param file - File to check
	 * @param md5 - remote MD5 to compare against
	 * @return boolean representing if it is valid
	 * @throws IOException 
	 */
	public static boolean isValid(File file, String md5) throws IOException {
		String result = fileMD5(file);
		Logger.logInfo("Local: " + result.toUpperCase());
		Logger.logInfo("Remote: " + md5.toUpperCase());
		return md5.equalsIgnoreCase(result);
	}

	/**
	 * Checks the file for corruption.
	 * @param file - File to check
	 * @param url - base url to grab md5 with old method
	 * @return boolean representing if it is valid
	 * @throws IOException 
	 */
	public static boolean backupIsValid(File file, String url) throws IOException {
		Logger.logInfo("Issue with new md5 method, attempting to use backup method.");
		String content = null;
		Scanner scanner = null;
		String resolved = (downloadServers.containsKey(Settings.getSettings().getDownloadServer())) ? "http://" + downloadServers.get(Settings.getSettings().getDownloadServer()) : "http://repo.feed-the-dojo.incraftion.com";
		resolved += "/md5/FTB2/" + url;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(resolved).openConnection();
			int response = connection.getResponseCode();
			if(response == 200) {
				scanner = new Scanner(connection.getInputStream());
				scanner.useDelimiter( "\\Z" );
				content = scanner.next();
			}
			if(response != 200 || (content == null || content.isEmpty())) {
				for(String server : backupServers.values()) {
					resolved = "http://" + server + "/md5/FTB2/" + url.replace("/", "%5E");
					connection = (HttpURLConnection) new URL(resolved).openConnection();
					response = connection.getResponseCode();
					if(response == 200) {
						scanner = new Scanner(connection.getInputStream());
						scanner.useDelimiter( "\\Z" );
						content = scanner.next();
						if(content != null && !content.isEmpty()) {
							break;
						}
					}
				}
			}
		} catch (IOException e) { 
		} finally {
			connection.disconnect();
			if (scanner != null) {
				scanner.close();
			}
		}
		String result = fileMD5(file);
		Logger.logInfo("Local: " + result.toUpperCase());
		Logger.logInfo("Remote: " + content.toUpperCase());
		return content.equalsIgnoreCase(result);
	}

	/**
	 * Gets the md5 of the downloaded file
	 * @param file - File to check
	 * @return - string of file's md5
	 * @throws IOException 
	 */
	public static String fileMD5(File file) throws IOException {
	    return fileHash(file, "md5");
	}
	public static String fileSHA(File file) throws IOException {
	    return fileHash(file, "sha1").toLowerCase();
	}
	public static String fileHash(File file, String type) throws IOException {
		if(!file.exists()) {
			return "";
		}
		URL fileUrl = file.toURI().toURL();
		MessageDigest dgest = null;
		try {
			dgest = MessageDigest.getInstance(type);
		} catch (NoSuchAlgorithmException e) { }
		InputStream str = fileUrl.openStream();
		byte[] buffer = new byte[65536];
		int readLen;
		while((readLen = str.read(buffer, 0, buffer.length)) != -1) {
			dgest.update(buffer, 0, readLen);
		}
		str.close();
		Formatter fmt = new Formatter();    
		for(byte b : dgest.digest()) { 
			fmt.format("%02X", b);    
		}
		String result = fmt.toString();
		fmt.close();
		return result;
	}

	/**
	 * Used to load all available download servers in a thread to prevent wait.
	 */
	@Override
	public void run() {
		downloadServers.put("Automatic", "repo.feed-the-dojo.incraftion.com");
		BufferedReader in = null;
		// New Servers
		try {
			in = new BufferedReader(new InputStreamReader(new URL("http://repo.feed-the-dojo.incraftion.com/edges.json").openStream()));
			String line;
			while((line = in.readLine()) != null) { // Hacky JSON parsing because this will all be gone soon (TM)
				line = line.replace("{", "").replace("}", "").replace("\"", "");
				String[] splitString = line.split(",");
				for(String entry : splitString) {
					String[] splitEntry = entry.split(":");
					if(splitEntry.length == 2) {
						downloadServers.put(splitEntry[0], splitEntry[1]);
					}
				}
			}
			in.close();
		} catch (IOException e) {
			Logger.logError(e.getMessage(), e);
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) { }
			}
		}
		
		// Backup md5 servers
		try {
			in = new BufferedReader(new InputStreamReader(new URL("http://feed-the-dojo.incraftion.com/mirrors").openStream()));
			String line;
			while((line = in.readLine()) != null) {
				String[] splitString = line.split(",");
				if(splitString.length == 2) {
					backupServers.put(splitString[0], splitString[1]);
				}
			}
			in.close();
		} catch (IOException e) {
			Logger.logError(e.getMessage(), e);
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) { }
			}
		}
		serversLoaded = true;
		if(LaunchFrame.getInstance() != null && LaunchFrame.getInstance().optionsPane != null) {
			AdvancedOptionsDialog.setDownloadServers();
		}
	}
}
