 /******************************************************************
 *
 *	CyberXML for Java
 *
 *	Copyright (C) Satoshi Konno 2002
 *
 *	File: Parser.java
 *
 *	Revision;
 *
 *	11/26/03
 *		- first revision.
 *	03/30/05
 *		- Change parse(String) to use StringBufferInputStream instead of URL.
 *
 ******************************************************************/
 
 package org.cybergarage.xml;
 
 import java.net.*;
 import java.io.*;
 
 public abstract class Parser 
 {
 	////////////////////////////////////////////////
 	//	Constructor
 	////////////////////////////////////////////////
 
 	public Parser()
 	{
 	}
 
 	////////////////////////////////////////////////
 	//	parse
 	////////////////////////////////////////////////
 
 	public abstract Node parse(InputStream inStream) throws ParserException;
 
 	////////////////////////////////////////////////
 	//	parse (URL)
 	////////////////////////////////////////////////
 
 	public Node parse(URL locationURL) throws ParserException
 	{
 		try {
 	 		HttpURLConnection urlCon = (HttpURLConnection)locationURL.openConnection();
 			// I2P mods to prevent hangs (see HTTPRequest for more info)
 			// this seems to work, getInputStream actually does the connect(),
 			// (as shown by a thread dump)
 			// so we can set these after openConnection()
 			// Alternative would be foo = new HttpURLConnection(locationURL); foo.set timeouts; foo.connect()
 			urlCon.setConnectTimeout(2*1000);
 			urlCon.setReadTimeout(1000);
 			urlCon.setRequestMethod("GET");
 			InputStream urlIn = urlCon.getInputStream();
 
 			Node rootElem = parse(urlIn);
 			
 			urlIn.close();
 			urlCon.disconnect();
 
 			return rootElem;
 			
 		} catch (Exception e) {
 			throw new ParserException(e);
 		}
 		/*
 		String host = locationURL.getHost();
 		int port = locationURL.getPort();
 		String uri = locationURL.getPath();
 		HTTPRequest httpReq = new HTTPRequest();
 		httpReq.setMethod(HTTP.GET);
 		httpReq.setURI(uri);
 		HTTPResponse httpRes = httpReq.post(host, port);
 		if (httpRes.isSuccessful() == false)
 			throw new ParserException(locationURL.toString());
 		String content = new String(httpRes.getContent());
 		StringBufferInputStream strBuf = new StringBufferInputStream(content);
 		return parse(strBuf);
 		*/
 	}
 
 	////////////////////////////////////////////////
 	//	parse (File)
 	////////////////////////////////////////////////
 
 	public Node parse(File descriptionFile) throws ParserException
 	{
 		try {
 			InputStream fileIn = new FileInputStream(descriptionFile);
 			Node root = parse(fileIn);
 			fileIn.close();
 			return root;
 			
 		} catch (Exception e) {
 			throw new ParserException(e);
 		}
 	}
 
 	////////////////////////////////////////////////
 	//	parse (Memory)
 	////////////////////////////////////////////////
 	
 	public Node parse(String descr) throws ParserException
 	{
 		try {
			InputStream decrIn = new ByteArrayInputStream(descr.getBytes());
 			Node root = parse(decrIn);
 			return root;
 		} catch (Exception e) {
 			throw new ParserException(e);
 		}
 	}
 
 }
 
 
