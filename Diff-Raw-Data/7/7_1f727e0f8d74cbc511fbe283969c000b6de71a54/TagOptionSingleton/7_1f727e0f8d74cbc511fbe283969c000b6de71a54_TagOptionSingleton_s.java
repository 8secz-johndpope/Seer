 /**
  *  Amended @author : Paul Taylor
  *  Initial @author : Eric Farng
  *
  *  Version @version:$Id$
  *
  *  MusicTag Copyright (C)2003,2004
  *
  *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
  *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
  *  or (at your option) any later version.
  *
  *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
  *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU Lesser General Public License for more details.
  *
  *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
  *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
  *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
  *
  * Description:
  * Options that are used for every datatype and class in this library.
  *
  */
 package org.jaudiotagger.tag;
 
 import org.jaudiotagger.tag.id3.framebody.AbstractID3v2FrameBody;
 import org.jaudiotagger.tag.id3.framebody.FrameBodyCOMM;
 import org.jaudiotagger.tag.id3.framebody.FrameBodyTIPL;
 import org.jaudiotagger.tag.id3.framebody.*;
 import org.jaudiotagger.tag.id3.valuepair.Languages;
 import org.jaudiotagger.tag.id3.valuepair.GenreTypes;
 import org.jaudiotagger.tag.TagException;
 import org.jaudiotagger.tag.lyrics3.Lyrics3v2Fields;
 import org.jaudiotagger.audio.AbstractAudioFile;
 
 import java.util.HashMap;
 import java.util.Iterator;
 import java.util.LinkedList;
 
 public class TagOptionSingleton
 {
     /**
      * DOCUMENT ME!
      */
     private static HashMap tagOptionTable = new HashMap();
 
     /**
      * DOCUMENT ME!
      */
     private static String DEFAULT = "default";
 
     /**
      * DOCUMENT ME!
      */
     private static Object defaultOptions = DEFAULT;
 
     /**
      * DOCUMENT ME!
      */
     private HashMap keywordMap = new HashMap();
 
     /**
      * Map of lyric ID's to Boolean objects if we should or should not save the
      * specific Kyrics3 field. Defaults to true.
      */
     private HashMap lyrics3SaveFieldMap = new HashMap();
 
     /**
      * parenthesis map stuff
      */
     private HashMap parenthesisMap = new HashMap();
 
     /**
      * <code>HashMap</code> listing words to be replaced if found
      */
     private HashMap replaceWordMap = new HashMap();
 
     /**
      * DOCUMENT ME!
      */
     private LinkedList endWordDelimiterList = new LinkedList();
 
     /**
      * delimiters within a file name
      */
     private LinkedList filenameDelimiterList = new LinkedList();
 
     /**
      * DOCUMENT ME!
      */
     private LinkedList startWordDelimiterList = new LinkedList();
 
     /**
      * words to always set the case as upper or lower case
      */
     private LinkedList upperLowerCaseWordList = new LinkedList();
 
     /**
      * default language for any ID3v2 tags frameswhich require it. This string
      * is in the [ISO-639-2] ISO/FDIS 639-2 definition
      */
     private String language = "eng";
 
     /**
      * DOCUMENT ME!
      */
     private boolean compositeMatchOverwrite = false;
 
     /**
      * DOCUMENT ME!
      */
     private boolean filenameTagSave = false;
 
     /**
      * if we should save any fields of the ID3v1 tag or not. Defaults to true.
      */
     private boolean id3v1Save = true;
 
     /**
      * if we should save the album field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveAlbum = true;
 
     /**
      * if we should save the artist field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveArtist = true;
 
     /**
      * if we should save the comment field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveComment = true;
 
     /**
      * if we should save the genre field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveGenre = true;
 
     /**
      * if we should save the title field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveTitle = true;
 
     /**
      * if we should save the track field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveTrack = true;
 
     /**
      * if we should save the year field of the ID3v1 tag or not. Defaults to
      * true.
      */
     private boolean id3v1SaveYear = true;
 
     /**
      * if we should keep an empty ID3v2 frame while we're reading. This is
      * different from a string of white space. Defaults to false.
      *
      * @todo I don't think I'm checking this right now
      */
     private boolean id3v2KeepEmptyFrameIfRead = false;
 
     /**
      * When adjusting the ID3v2 padding, if should we copy the current ID3v2
      * tag to the new MP3 file. Defaults to true.
      */
     private boolean id3v2PaddingCopyTag = true;
 
     /**
      * When adjusting the ID3v2 padding, if we should shorten the length of the
      * ID3v2 tag padding. Defaults to false.
      */
     private boolean id3v2PaddingWillShorten = false;
 
     /**
      * if we should save any fields of the ID3v2 tag or not. Defaults to true.
      */
     private boolean id3v2Save = true;
 
     /**
      * if we should save empty ID3v2 frames or not. Defaults to false.
      *
      * @todo I don't think this is implemented yet.
      */
     private boolean id3v2SaveEmptyFrame = false;
 
     /**
      * if we should save the ID3v2 extended header or not. Defaults to false.
      *
      * @todo Not implemented yet
      */
     private boolean id3v2SaveExtendedHeader = false;
 
     /**
      * if we should keep an empty Lyrics3 field while we're reading. This is
      * different from a string of white space. Defaults to false.
      */
     private boolean lyrics3KeepEmptyFieldIfRead = false;
 
     /**
      * if we should save any fields of the Lyrics3 tag or not. Defaults to
      * true.
      */
     private boolean lyrics3Save = true;
 
     /**
      * if we should save empty Lyrics3 field or not. Defaults to false.
      *
      * @todo I don't think this is implemented yet.
      */
     private boolean lyrics3SaveEmptyField = false;
 
     /**
      * DOCUMENT ME!
      */
     private boolean originalSavedAfterAdjustingID3v2Padding = true;
 
     /**
      * default play counter size in bytes for the ID3v2 Tag.
      *
      * @todo implement this.
      */
     private byte playCounterSize = 4;
 
     /**
      * default text encoding for any ID3v2 tag frames which require it.
      */
     private byte textEncoding = 0;
 
     /**
      * default time stamp format for any ID3v2 tag frames which require it.
      */
     private byte timeStampFormat = 2;
 
     /**
      * factor to increase the id3v2 padding size. When the ID3v2 tag padding
      * length is calculated and is not large enough to fit the current ID3v2
      * tag, the padding length will be multiplied by this number until it is
      * large enough.
      */
     private float id3v2PaddingMultiplier = 2;
 
     /**
      * DOCUMENT ME!
      */
    private int defaultSaveMode = AbstractAudioFile.MP3_FILE_SAVE_WRITE;
 
     /**
      * padding length of the ID3v2 tag.
      */
     private int id3v2PaddingSize = 2048;
 
     /**
      * number of frames to sync when trying to find the start of the MP3 frame
      * data. The start of the MP3 frame data is the start of the music and is
      * different from the ID3v2 frame data.
      */
     private int numberMP3SyncFrame = 3;
 
     /**
      * Creates a new TagOptions datatype. All Options are set to their default
      * values
      */
     private TagOptionSingleton()
     {
         setToDefault();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public static TagOptionSingleton getInstance()
     {
         return getInstance(defaultOptions);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param instanceKey DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public static TagOptionSingleton getInstance(Object instanceKey)
     {
         TagOptionSingleton tagOptions = (TagOptionSingleton) tagOptionTable.get(instanceKey);
 
         if (tagOptions == null)
         {
             tagOptions = new TagOptionSingleton();
             tagOptionTable.put(instanceKey, tagOptions);
         }
 
         return tagOptions;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param open DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public String getCloseParenthesis(String open)
     {
         return (String) parenthesisMap.get(open);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param close DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public boolean isCloseParenthesis(String close)
     {
         return parenthesisMap.containsValue(close);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param compositeMatchOverwrite DOCUMENT ME!
      */
     public void setCompositeMatchOverwrite(boolean compositeMatchOverwrite)
     {
         this.compositeMatchOverwrite = compositeMatchOverwrite;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isCompositeMatchOverwrite()
     {
         return compositeMatchOverwrite;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param defaultSaveMode DOCUMENT ME!
      */
     public void setDefaultSaveMode(int defaultSaveMode)
     {
         this.defaultSaveMode = defaultSaveMode;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public int getDefaultSaveMode()
     {
         return defaultSaveMode;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param filenameTagSave DOCUMENT ME!
      */
     public void setFilenameTagSave(boolean filenameTagSave)
     {
         this.filenameTagSave = filenameTagSave;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isFilenameTagSave()
     {
         return filenameTagSave;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2SaveExtendedHeader()
     {
         return id3v2SaveExtendedHeader;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param instanceKey DOCUMENT ME!
      */
     public void setInstanceKey(Object instanceKey)
     {
         TagOptionSingleton.defaultOptions = instanceKey;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public static Object getInstanceKey()
     {
         return defaultOptions;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getEndWordDelimiterIterator()
     {
         return endWordDelimiterList.iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getFilenameDelimiterIterator()
     {
         return filenameDelimiterList.iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1Save DOCUMENT ME!
      */
     public void setId3v1Save(boolean id3v1Save)
     {
         this.id3v1Save = id3v1Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1Save()
     {
         return id3v1Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveAlbum DOCUMENT ME!
      */
     public void setId3v1SaveAlbum(boolean id3v1SaveAlbum)
     {
         this.id3v1SaveAlbum = id3v1SaveAlbum;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveAlbum()
     {
         return id3v1SaveAlbum;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveArtist DOCUMENT ME!
      */
     public void setId3v1SaveArtist(boolean id3v1SaveArtist)
     {
         this.id3v1SaveArtist = id3v1SaveArtist;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveArtist()
     {
         return id3v1SaveArtist;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveComment DOCUMENT ME!
      */
     public void setId3v1SaveComment(boolean id3v1SaveComment)
     {
         this.id3v1SaveComment = id3v1SaveComment;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveComment()
     {
         return id3v1SaveComment;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveGenre DOCUMENT ME!
      */
     public void setId3v1SaveGenre(boolean id3v1SaveGenre)
     {
         this.id3v1SaveGenre = id3v1SaveGenre;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveGenre()
     {
         return id3v1SaveGenre;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveTitle DOCUMENT ME!
      */
     public void setId3v1SaveTitle(boolean id3v1SaveTitle)
     {
         this.id3v1SaveTitle = id3v1SaveTitle;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveTitle()
     {
         return id3v1SaveTitle;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveTrack DOCUMENT ME!
      */
     public void setId3v1SaveTrack(boolean id3v1SaveTrack)
     {
         this.id3v1SaveTrack = id3v1SaveTrack;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveTrack()
     {
         return id3v1SaveTrack;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v1SaveYear DOCUMENT ME!
      */
     public void setId3v1SaveYear(boolean id3v1SaveYear)
     {
         this.id3v1SaveYear = id3v1SaveYear;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v1SaveYear()
     {
         return id3v1SaveYear;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2KeepEmptyFrameIfRead DOCUMENT ME!
      */
     public void setId3v2KeepEmptyFrameIfRead(boolean id3v2KeepEmptyFrameIfRead)
     {
         this.id3v2KeepEmptyFrameIfRead = id3v2KeepEmptyFrameIfRead;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2KeepEmptyFrameIfRead()
     {
         return id3v2KeepEmptyFrameIfRead;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2PaddingCopyTag DOCUMENT ME!
      */
     public void setId3v2PaddingCopyTag(boolean id3v2PaddingCopyTag)
     {
         this.id3v2PaddingCopyTag = id3v2PaddingCopyTag;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2PaddingCopyTag()
     {
         return id3v2PaddingCopyTag;
     }
 
     /**
      * Sets the factor to increase the id3v2 padding size. When the ID3v2 tag
      * padding length is calculated and is not large enough to fit the current
      * ID3v2 tag, the padding length will be multiplied by this number until
      * it is large enough.
      *
      * @param mult new factor to increase the id3v2 padding size.
      */
     public void setId3v2PaddingMultiplier(float mult)
     {
         if (mult > 1)
         {
             id3v2PaddingMultiplier = mult;
         }
     }
 
     /**
      * Returns the factor to increase the id3v2 padding size. When the ID3v2
      * tag padding length is calculated and is not large enough to fit the
      * current ID3v2 tag, the padding length will be multiplied by this number
      * until it is large enough.
      *
      * @return the factor to increase the id3v2 padding size
      */
     public float getId3v2PaddingMultiplier()
     {
         return id3v2PaddingMultiplier;
     }
 
     /**
      * Sets the initial ID3v2 padding length. This will be the minimum padding
      * length of the ID3v2 tag. The <code>willShorten</code> setting will not
      * make the length shorter than this value.
      *
      * @param size the new initial ID3v2 padding length
      */
     public void setId3v2PaddingSize(int size)
     {
         if (size >= 0)
         {
             id3v2PaddingSize = size;
         }
     }
 
     /**
      * Returns the initial ID3v2 padding length. This will be the minimum
      * padding length of the ID3v2 tag. The <code>willShorten</code> setting
      * will not make the length shorter than this value.
      *
      * @return the initial ID3v2 padding length
      */
     public int getId3v2PaddingSize()
     {
         return id3v2PaddingSize;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2PaddingWillShorten DOCUMENT ME!
      */
     public void setId3v2PaddingWillShorten(boolean id3v2PaddingWillShorten)
     {
         this.id3v2PaddingWillShorten = id3v2PaddingWillShorten;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2PaddingWillShorten()
     {
         return id3v2PaddingWillShorten;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2Save DOCUMENT ME!
      */
     public void setId3v2Save(boolean id3v2Save)
     {
         this.id3v2Save = id3v2Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2Save()
     {
         return id3v2Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2SaveEmptyFrame DOCUMENT ME!
      */
     public void setId3v2SaveEmptyFrame(boolean id3v2SaveEmptyFrame)
     {
         this.id3v2SaveEmptyFrame = id3v2SaveEmptyFrame;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isId3v2SaveEmptyFrame()
     {
         return id3v2SaveEmptyFrame;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2SaveExtendedHeader DOCUMENT ME!
      */
     public void setId3v2SaveExtendedHeader(boolean id3v2SaveExtendedHeader)
     {
         this.id3v2SaveExtendedHeader = id3v2SaveExtendedHeader;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getKeywordIterator()
     {
         return keywordMap.keySet().iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2_4FrameBody DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public Iterator getKeywordListIterator(Class id3v2_4FrameBody)
     {
         return ((LinkedList) keywordMap.get(id3v2_4FrameBody)).iterator();
     }
 
     /**
      * Sets the default language for any ID3v2 tag frames which require it.
      * While the value will already exist when reading from a file, this value
      * will be used when a new ID3v2 Frame is created from scratch.
      *
      * @param lang language ID, [ISO-639-2] ISO/FDIS 639-2 definition
      */
     public void setLanguage(String lang)
     {
         if (Languages.getInstanceOf().getIdToValueMap().containsKey(lang))
         {
             language = lang;
         }
     }
 
     /**
      * Returns the default language for any ID3v2 tag frames which require it.
      *
      * @return language ID, [ISO-639-2] ISO/FDIS 639-2 definition
      */
     public String getLanguage()
     {
         return language;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param lyrics3KeepEmptyFieldIfRead DOCUMENT ME!
      */
     public void setLyrics3KeepEmptyFieldIfRead(boolean lyrics3KeepEmptyFieldIfRead)
     {
         this.lyrics3KeepEmptyFieldIfRead = lyrics3KeepEmptyFieldIfRead;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isLyrics3KeepEmptyFieldIfRead()
     {
         return lyrics3KeepEmptyFieldIfRead;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param lyrics3Save DOCUMENT ME!
      */
     public void setLyrics3Save(boolean lyrics3Save)
     {
         this.lyrics3Save = lyrics3Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isLyrics3Save()
     {
         return lyrics3Save;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param lyrics3SaveEmptyField DOCUMENT ME!
      */
     public void setLyrics3SaveEmptyField(boolean lyrics3SaveEmptyField)
     {
         this.lyrics3SaveEmptyField = lyrics3SaveEmptyField;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isLyrics3SaveEmptyField()
     {
         return lyrics3SaveEmptyField;
     }
 
     /**
      * Sets if we should save the Lyrics3 field. Defaults to true.
      *
      * @param id   Lyrics3 id string
      * @param save true if you want to save this specific Lyrics3 field.
      */
     public void setLyrics3SaveField(String id, boolean save)
     {
         this.lyrics3SaveFieldMap.put(id, new Boolean(save));
     }
 
     /**
      * Returns true if we should save the Lyrics3 field asked for in the
      * argument. Defaults to true.
      *
      * @param id Lyrics3 id string
      * @return true if we should save the Lyrics3 field.
      */
     public boolean getLyrics3SaveField(String id)
     {
         return ((Boolean) lyrics3SaveFieldMap.get(id)).booleanValue();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public HashMap getLyrics3SaveFieldMap()
     {
         return lyrics3SaveFieldMap;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param oldWord DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public String getNewReplaceWord(String oldWord)
     {
         return (String) replaceWordMap.get(oldWord);
     }
 
     /**
      * Sets the number of MP3 frames to sync when trying to find the start of
      * the MP3 frame data. The start of the MP3 frame data is the start of the
      * music and is different from the ID3v2 frame data. WinAmp 2.8 seems to
      * sync 3 frames. Default is 5.
      *
      * @param numberMP3SyncFrame number of MP3 frames to sync
      */
     public void setNumberMP3SyncFrame(int numberMP3SyncFrame)
     {
         this.numberMP3SyncFrame = numberMP3SyncFrame;
     }
 
     /**
      * Returns the number of MP3 frames to sync when trying to find the start
      * of the MP3 frame data. The start of the MP3 frame data is the start of
      * the music and is different from the ID3v2 frame data. WinAmp 2.8 seems
      * to sync 3 frames. Default is 5.
      *
      * @return number of MP3 frames to sync
      */
     public int getNumberMP3SyncFrame()
     {
         return numberMP3SyncFrame;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getOldReplaceWordIterator()
     {
         return replaceWordMap.keySet().iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param open DOCUMENT ME!
      * @return DOCUMENT ME!
      */
     public boolean isOpenParenthesis(String open)
     {
         return parenthesisMap.containsKey(open);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getOpenParenthesisIterator()
     {
         return parenthesisMap.keySet().iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param originalSavedAfterAdjustingID3v2Padding
      *         DOCUMENT ME!
      */
     public void setOriginalSavedAfterAdjustingID3v2Padding(boolean originalSavedAfterAdjustingID3v2Padding)
     {
         this.originalSavedAfterAdjustingID3v2Padding = originalSavedAfterAdjustingID3v2Padding;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public boolean isOriginalSavedAfterAdjustingID3v2Padding()
     {
         return originalSavedAfterAdjustingID3v2Padding;
     }
 
     /**
      * Sets the default play counter size for the PCNT ID3v2 frame. While the
      * value will already exist when reading from a file, this value will be
      * used when a new ID3v2 Frame is created from scratch.
      *
      * @param size the default play counter size for the PCNT ID3v2 frame
      */
     public void setPlayCounterSize(byte size)
     {
         if (size > 0)
         {
             playCounterSize = size;
         }
     }
 
     /**
      * Returns the default play counter size for the PCNT ID3v2 frame.
      *
      * @return the default play counter size for the PCNT ID3v2 frame
      */
     public byte getPlayCounterSize()
     {
         return playCounterSize;
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getStartWordDelimiterIterator()
     {
         return startWordDelimiterList.iterator();
     }
 
     /**
      * Sets the default text encoding for any ID3v2 tag frames which require
      * it. While the value will already exist when reading from a file, this
      * value will be used when a new ID3v2 Frame is created from scratch.
      * <p/>
      * <P>
      * $00   ISO-8859-1 [ISO-8859-1]. Terminated with $00.<BR> $01   UTF-16
      * [UTF-16] encoded Unicode [UNICODE] with BOM. All strings in the same
      * frame SHALL have the same byteorder. Terminated with $00 00.<BR> $02
      * UTF-16BE [UTF-16] encoded Unicode [UNICODE] without BOM. Terminated
      * with $00 00.<BR> $03   UTF-8 [UTF-8] encoded Unicode [UNICODE].
      * Terminated with $00.<BR>
      * </p>
      *
      * @param enc new default text encoding
      */
     public void setTextEncoding(byte enc)
     {
         if ((enc >= 0) && (enc <= 3))
         {
             textEncoding = enc;
         }
     }
 
     /**
      * Returns the default text encoding format for ID3v2 tags which require
      * it.
      * <p/>
      * <P>
      * $00   ISO-8859-1 [ISO-8859-1]. Terminated with $00.<BR> $01   UTF-16
      * [UTF-16] encoded Unicode [UNICODE] with BOM. All strings in the same
      * frame SHALL have the same byteorder. Terminated with $00 00.<BR> $02
      * UTF-16BE [UTF-16] encoded Unicode [UNICODE] without BOM. Terminated
      * with $00 00.<BR> $03   UTF-8 [UTF-8] encoded Unicode [UNICODE].
      * Terminated with $00.<BR>
      * </p>
      *
      * @return the default text encoding
      */
     public byte getTextEncoding()
     {
         return textEncoding;
     }
 
     /**
      * Sets the default time stamp format for ID3v2 tags which require it.
      * While the value will already exist when reading from a file, this value
      * will be used when a new ID3v2 Frame is created from scratch.
      * <p/>
      * <P>
      * $01  Absolute time, 32 bit sized, using MPEG frames as unit<br>
      * $02  Absolute time, 32 bit sized, using milliseconds as unit<br>
      * </p>
      *
      * @param tsf the new default time stamp format
      */
     public void setTimeStampFormat(byte tsf)
     {
         if ((tsf == 1) || (tsf == 2))
         {
             timeStampFormat = tsf;
         }
     }
 
     /**
      * Returns the default time stamp format for ID3v2 tags which require it.
      * <p/>
      * <P>
      * $01  Absolute time, 32 bit sized, using MPEG frames as unit<br>
      * $02  Absolute time, 32 bit sized, using milliseconds as unit<br>
      * </p>
      *
      * @return the default time stamp format
      */
     public byte getTimeStampFormat()
     {
         return timeStampFormat;
     }
 
     /**
      * DOCUMENT ME!
      */
     public void setToDefault()
     {
         keywordMap = new HashMap();
 
         compositeMatchOverwrite = false;
        defaultSaveMode = AbstractAudioFile.MP3_FILE_SAVE_WRITE;
         endWordDelimiterList = new LinkedList();
         filenameDelimiterList = new LinkedList();
         filenameTagSave = false;
         id3v1Save = true;
         id3v1SaveAlbum = true;
         id3v1SaveArtist = true;
         id3v1SaveComment = true;
         id3v1SaveGenre = true;
         id3v1SaveTitle = true;
         id3v1SaveTrack = true;
         id3v1SaveYear = true;
         id3v2KeepEmptyFrameIfRead = false;
         id3v2PaddingCopyTag = true;
         id3v2PaddingWillShorten = false;
         id3v2Save = true;
         id3v2SaveEmptyFrame = false;
         id3v2SaveExtendedHeader = false;
         id3v2PaddingMultiplier = 2;
         id3v2PaddingSize = 2048;
         language = "eng";
         lyrics3KeepEmptyFieldIfRead = false;
         lyrics3Save = true;
         lyrics3SaveEmptyField = false;
         lyrics3SaveFieldMap = new HashMap();
         numberMP3SyncFrame = 3;
         parenthesisMap = new HashMap();
         playCounterSize = 4;
         replaceWordMap = new HashMap();
         startWordDelimiterList = new LinkedList();
         textEncoding = 0;
         timeStampFormat = 2;
         upperLowerCaseWordList = new LinkedList();
 
         /**
          * default all lyrics3 fields to save. id3v1 fields are individual
          * settings. id3v2 fields are always looked at to save.
          */
         Iterator iterator = Lyrics3v2Fields.getInstanceOf().getIdToValueMap().keySet().iterator();
         String fieldId;
 
         while (iterator.hasNext())
         {
             fieldId = (String) iterator.next();
             lyrics3SaveFieldMap.put(fieldId, new Boolean(true));
         }
 
         try
         {
             addKeyword(FrameBodyCOMM.class, "ultimix");
             addKeyword(FrameBodyCOMM.class, "dance");
             addKeyword(FrameBodyCOMM.class, "mix");
             addKeyword(FrameBodyCOMM.class, "remix");
             addKeyword(FrameBodyCOMM.class, "rmx");
             addKeyword(FrameBodyCOMM.class, "live");
             addKeyword(FrameBodyCOMM.class, "cover");
             addKeyword(FrameBodyCOMM.class, "soundtrack");
             addKeyword(FrameBodyCOMM.class, "version");
             addKeyword(FrameBodyCOMM.class, "acoustic");
             addKeyword(FrameBodyCOMM.class, "original");
             addKeyword(FrameBodyCOMM.class, "cd");
             addKeyword(FrameBodyCOMM.class, "extended");
             addKeyword(FrameBodyCOMM.class, "vocal");
             addKeyword(FrameBodyCOMM.class, "unplugged");
             addKeyword(FrameBodyCOMM.class, "acapella");
             addKeyword(FrameBodyCOMM.class, "edit");
             addKeyword(FrameBodyCOMM.class, "radio");
             addKeyword(FrameBodyCOMM.class, "original");
             addKeyword(FrameBodyCOMM.class, "album");
             addKeyword(FrameBodyCOMM.class, "studio");
             addKeyword(FrameBodyCOMM.class, "instrumental");
             addKeyword(FrameBodyCOMM.class, "unedited");
             addKeyword(FrameBodyCOMM.class, "karoke");
             addKeyword(FrameBodyCOMM.class, "quality");
             addKeyword(FrameBodyCOMM.class, "uncensored");
             addKeyword(FrameBodyCOMM.class, "clean");
             addKeyword(FrameBodyCOMM.class, "dirty");
 
             addKeyword(FrameBodyTIPL.class, "f.");
             addKeyword(FrameBodyTIPL.class, "feat");
             addKeyword(FrameBodyTIPL.class, "feat.");
             addKeyword(FrameBodyTIPL.class, "featuring");
             addKeyword(FrameBodyTIPL.class, "ftng");
             addKeyword(FrameBodyTIPL.class, "ftng.");
             addKeyword(FrameBodyTIPL.class, "ft.");
             addKeyword(FrameBodyTIPL.class, "ft");
 
             iterator = GenreTypes.getInstanceOf().getValueToIdMap().keySet().iterator();
 
             while (iterator.hasNext())
             {
                 addKeyword(FrameBodyCOMM.class, (String) iterator.next());
             }
         }
         catch (TagException ex)
         {
             // this shouldn't happen. if it does, we should fix it right away.
             ex.printStackTrace();
         }
 
         addUpperLowerCaseWord("a");
         addUpperLowerCaseWord("in");
         addUpperLowerCaseWord("of");
         addUpperLowerCaseWord("the");
         addUpperLowerCaseWord("on");
         addUpperLowerCaseWord("is");
         addUpperLowerCaseWord("it");
         addUpperLowerCaseWord("to");
         addUpperLowerCaseWord("at");
         addUpperLowerCaseWord("an");
         addUpperLowerCaseWord("and");
         addUpperLowerCaseWord("but");
         addUpperLowerCaseWord("or");
         addUpperLowerCaseWord("for");
         addUpperLowerCaseWord("nor");
         addUpperLowerCaseWord("not");
         addUpperLowerCaseWord("so");
         addUpperLowerCaseWord("yet");
         addUpperLowerCaseWord("with");
         addUpperLowerCaseWord("into");
         addUpperLowerCaseWord("by");
         addUpperLowerCaseWord("up");
         addUpperLowerCaseWord("as");
         addUpperLowerCaseWord("if");
         addUpperLowerCaseWord("feat.");
         addUpperLowerCaseWord("vs.");
         addUpperLowerCaseWord("I'm");
         addUpperLowerCaseWord("I");
         addUpperLowerCaseWord("I've");
         addUpperLowerCaseWord("I'll");
 
         addReplaceWord("v.", "vs.");
         addReplaceWord("vs.", "vs.");
         addReplaceWord("versus", "vs.");
         addReplaceWord("f.", "feat.");
         addReplaceWord("feat", "feat.");
         addReplaceWord("featuring", "feat.");
         addReplaceWord("ftng.", "feat.");
         addReplaceWord("ftng", "feat.");
         addReplaceWord("ft.", "feat.");
         addReplaceWord("ft", "feat.");
 
         addFilenameDelimiter("/");
         addFilenameDelimiter("\\");
         addFilenameDelimiter(" -");
         addFilenameDelimiter(";");
         addFilenameDelimiter("|");
         addFilenameDelimiter(":");
 
         iterator = this.getKeywordListIterator(FrameBodyTIPL.class);
 
         while (iterator.hasNext())
         {
             addStartWordDelimiter((String) iterator.next());
         }
 
         addParenthesis("(", ")");
         addParenthesis("[", "]");
         addParenthesis("{", "}");
         addParenthesis("<", ">");
     }
 
     /**
      * DOCUMENT ME!
      *
      * @return DOCUMENT ME!
      */
     public Iterator getUpperLowerCaseWordListIterator()
     {
         return upperLowerCaseWordList.iterator();
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param wordDelimiter DOCUMENT ME!
      */
     public void addEndWordDelimiter(String wordDelimiter)
     {
         endWordDelimiterList.add(wordDelimiter);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param delimiter DOCUMENT ME!
      */
     public void addFilenameDelimiter(String delimiter)
     {
         filenameDelimiterList.add(delimiter);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param id3v2FrameBodyClass DOCUMENT ME!
      * @param keyword             DOCUMENT ME!
      * @throws TagException DOCUMENT ME!
      */
     public void addKeyword(Class id3v2FrameBodyClass, String keyword)
         throws TagException
     {
         if (AbstractID3v2FrameBody.class.isAssignableFrom(id3v2FrameBodyClass) == false)
         {
             throw new TagException("Invalid class type. Must be AbstractId3v2FrameBody " + id3v2FrameBodyClass);
         }
 
         if ((keyword != null) && (keyword.length() > 0))
         {
             LinkedList keywordList;
 
             if (keywordMap.containsKey(id3v2FrameBodyClass) == false)
             {
                 keywordList = new LinkedList();
                 keywordMap.put(id3v2FrameBodyClass, keywordList);
             }
             else
             {
                 keywordList = (LinkedList) keywordMap.get(id3v2FrameBodyClass);
             }
 
             keywordList.add(keyword);
         }
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param open  DOCUMENT ME!
      * @param close DOCUMENT ME!
      */
     public void addParenthesis(String open, String close)
     {
         parenthesisMap.put(open, close);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param oldWord DOCUMENT ME!
      * @param newWord DOCUMENT ME!
      */
     public void addReplaceWord(String oldWord, String newWord)
     {
         replaceWordMap.put(oldWord, newWord);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param wordDelimiter DOCUMENT ME!
      */
     public void addStartWordDelimiter(String wordDelimiter)
     {
         startWordDelimiterList.add(wordDelimiter);
     }
 
     /**
      * DOCUMENT ME!
      *
      * @param word DOCUMENT ME!
      */
     public void addUpperLowerCaseWord(String word)
     {
         upperLowerCaseWordList.add(word);
     }
 }
