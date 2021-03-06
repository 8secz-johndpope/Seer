 package net.dandielo.citizens.trader.locale;
 
 public class LocaleEntry {	
 	//for updater
 	private String newkey;
 	
 	//the locale system
 	private String ver;
 	private String key;
 	
 	public LocaleEntry(String key, String ver) {
 		this.key = key;
 		this.ver = ver;
 	}
 	
 	public LocaleEntry(String key, String newkey, String ver) {
 		this.key = key;
 		this.ver = ver;
 		this.newkey = newkey;
 	}
 	
 	public boolean hasNewkey()
 	{
 		return !newkey.isEmpty();
 	}
 	
 	public String newkey()
 	{
 		return newkey.isEmpty() ? key : newkey;
 	}
 	
 	public String key()
 	{
 		return key;
 	}
 	
 	public String ver()
 	{
 		return ver;
 	}
 	
 	@Override
 	public String toString()
 	{
 		return key;
 	}
 	
 	@Override
 	public int hashCode()
 	{
		System.out.print(key);
 		return key.hashCode();
 	}
 	
 	@Override 
 	public boolean equals(Object o)
 	{
 		return key.equals(((LocaleEntry)o).key);
 	}
 }
