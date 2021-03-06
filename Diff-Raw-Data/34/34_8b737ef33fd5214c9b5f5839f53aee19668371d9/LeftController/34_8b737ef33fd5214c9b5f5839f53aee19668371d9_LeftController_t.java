 package net.sourceforge.subsonic.controller;
 
 import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
 import net.sourceforge.subsonic.domain.MusicFile;
 import net.sourceforge.subsonic.domain.MusicFolder;
 import net.sourceforge.subsonic.domain.MusicIndex;
 import net.sourceforge.subsonic.domain.UserSettings;
 import net.sourceforge.subsonic.service.MusicFileService;
 import net.sourceforge.subsonic.service.SearchService;
 import net.sourceforge.subsonic.service.SecurityService;
 import net.sourceforge.subsonic.service.SettingsService;
 import net.sourceforge.subsonic.service.MusicIndexService;
 import net.sourceforge.subsonic.util.StringUtil;
 import org.springframework.web.servlet.ModelAndView;
 import org.springframework.web.servlet.mvc.LastModified;
 import org.springframework.web.servlet.mvc.ParameterizableViewController;
 import org.springframework.web.servlet.support.RequestContextUtils;
 
 import javax.servlet.http.HttpServletRequest;
 import javax.servlet.http.HttpServletResponse;
 import java.io.File;
import java.io.IOException;
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.SortedSet;
 import java.util.SortedMap;
 
 /**
  * Controller for the left index frame.
  *
  * @author Sindre Mehus
  */
 public class LeftController extends ParameterizableViewController implements LastModified {
 
     private SearchService searchService;
     private SettingsService settingsService;
     private SecurityService securityService;
     private MusicFileService musicFileService;
     private MusicIndexService musicIndexService;
 
     /**
      * {@inheritDoc}
      */
     public long getLastModified(HttpServletRequest request) {
         saveSelectedMusicFolder(request);
         MusicFolder[] musicFolders = settingsService.getAllMusicFolders();
 
         long lastModified = settingsService.getSettingsLastChanged();
         for (MusicFolder musicFolder : musicFolders) {
             File file = musicFolder.getPath();
             lastModified = Math.max(lastModified, file.lastModified());
         }
 
         return lastModified;
     }
 
     /**
      * {@inheritDoc}
      */
     @Override
     protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
         saveSelectedMusicFolder(request);
         Map<String, Object> map = new HashMap<String, Object>();
 
         MediaLibraryStatistics statistics = searchService.getStatistics();
         Locale locale = RequestContextUtils.getLocale(request);
 
         MusicFolder[] allMusicFolders = settingsService.getAllMusicFolders();
         MusicFolder selectedMusicFolder = getSelectedMusicFolder(request);
         MusicFolder[] musicFoldersToUse = selectedMusicFolder == null ? allMusicFolders : new MusicFolder[]{selectedMusicFolder};
         String[] shortcuts = settingsService.getShortcutsAsArray();
         SortedMap<MusicIndex, SortedSet<MusicIndex.Artist>> indexedArtists = musicIndexService.getIndexedArtists(musicFoldersToUse);
        List<MusicFile> singleSongs = getSingleSongs(musicFoldersToUse);
         UserSettings userSettings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
 
         map.put("musicFolders", allMusicFolders);
         map.put("selectedMusicFolder", selectedMusicFolder);
         map.put("radios", settingsService.getAllInternetRadios());
         map.put("shortcuts", getShortcuts(musicFoldersToUse, shortcuts));
         map.put("captionCutoff", userSettings.getMainVisibility().getCaptionCutoff());
         map.put("partyMode", userSettings.isPartyModeEnabled());
 
         if (statistics != null) {
             map.put("statistics", statistics);
             long bytes = statistics.getTotalLengthInBytes();
             long hours = bytes * 8 / 1024 / 150 / 3600;
             map.put("hours", hours);
             map.put("bytes", StringUtil.formatBytes(bytes, locale));
         }
 
         map.put("indexedArtists", indexedArtists);
        map.put("singleSongs", singleSongs);
         map.put("indexes", indexedArtists.keySet());
         map.put("downloadEnabled", securityService.getCurrentUser(request).isDownloadRole());
 
         ModelAndView result = super.handleRequestInternal(request, response);
         result.addObject("model", map);
         return result;
     }
 
     private void saveSelectedMusicFolder(HttpServletRequest request) {
         if (request.getParameter("musicFolderId") == null) {
             return;
         }
         int musicFolderId = Integer.parseInt(request.getParameter("musicFolderId"));
 
         UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
         settings.setSelectedMusicFolderId(musicFolderId);
         settingsService.updateUserSettings(settings);
     }
 
     /**
      * Returns the selected music folder, or <code>null</code> if all music folders should be displayed.
      */
     private MusicFolder getSelectedMusicFolder(HttpServletRequest request) {
         UserSettings settings = settingsService.getUserSettings(securityService.getCurrentUsername(request));
         int musicFolderId = settings.getSelectedMusicFolderId();
 
         return settingsService.getMusicFolderById(musicFolderId);
     }
 
    private List<MusicFile> getSingleSongs(MusicFolder[] folders) throws IOException {
        List<MusicFile> result = new ArrayList<MusicFile>();
        for (MusicFolder folder : folders) {
            MusicFile parent = musicFileService.getMusicFile(folder.getPath());
            result.addAll(parent.getChildren(false, true));
        }
        return result;
    }

     private List<MusicFile> getShortcuts(MusicFolder[] musicFoldersToUse, String[] shortcuts) {
         List<MusicFile> result = new ArrayList<MusicFile>();
 
         for (String shortcut : shortcuts) {
             for (MusicFolder musicFolder : musicFoldersToUse) {
                 File file = new File(musicFolder.getPath(), shortcut);
                 if (file.exists()) {
                     result.add(musicFileService.getMusicFile(file));
                 }
             }
         }
 
         return result;
     }
 
     public void setSearchService(SearchService searchService) {
         this.searchService = searchService;
     }
 
     public void setSettingsService(SettingsService settingsService) {
         this.settingsService = settingsService;
     }
 
     public void setSecurityService(SecurityService securityService) {
         this.securityService = securityService;
     }
 
     public void setMusicFileService(MusicFileService musicFileService) {
         this.musicFileService = musicFileService;
     }
 
     public void setMusicIndexService(MusicIndexService musicIndexService) {
         this.musicIndexService = musicIndexService;
     }
 }
