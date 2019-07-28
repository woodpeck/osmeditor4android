package de.blau.android.prefs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Urls;
import de.blau.android.osm.Server;
import de.blau.android.presets.Preset;
import de.blau.android.resources.DataStyle;

/**
 * Convenience class for parsing and holding the application's SharedPreferences.
 * 
 * @author mb
 */
public class Preferences {
    private static String DEBUG_TAG = "Preferences";

    private final AdvancedPrefDatabase advancedPrefs;

    private final boolean isStatsVisible;
    private final boolean isToleranceVisible;
    private final boolean isAntiAliasingEnabled;
    private boolean       isOpenStreetBugsEnabled;
    private boolean       isPhotoLayerEnabled;
    private final boolean isKeepScreenOnEnabled;
    private final boolean useBackForUndo;
    private final boolean largeDragArea;
    private final boolean tagFormEnabled;
    private String        backgroundLayer;
    private String        overlayLayer;
    private String        scaleLayer;
    private final String  mapProfile;
    private final String  followGPSbutton;
    private final String  fullscreenMode;
    private final String  mapOrientation;
    private int           gpsInterval;
    private float         gpsDistance;
    private float         maxStrokeWidth;
    private int           tileCacheSize;                 // in MB
    private int           downloadRadius;                // in m
    private float         maxDownloadSpeed;              // in km/h
    private int           bugDownloadRadius;
    private float         maxBugDownloadSpeed;           // in km/h
    private Set<String>   taskFilter;                    // can't be final
    private final boolean forceContextMenu;
    private final boolean enableNameSuggestions;
    private final boolean nameSuggestionPresetsEnabled;
    private final boolean closeChangesetOnSave;
    private final boolean splitActionBarEnabled;
    private final String  gpsSource;
    private final String  gpsTcpSource;
    private final String  offsetServer;
    private final String  osmoseServer;
    private final String  mapRouletteServer;
    private String        taginfoServer;
    private final boolean showCameraAction;
    private final boolean useInternalPhotoViewer;
    private final boolean generateAlerts;
    private final boolean groupAlertsOnly;
    private int           maxAlertDistance;
    private final boolean lightThemeEnabled;
    private Set<String>   addressTags;                   // can't be final
    private final boolean voiceCommandsEnabled;
    private final boolean leaveGpsDisabled;
    private final boolean allowFallbackToNetworkLocation;
    private final boolean showIcons;
    private final boolean showWayIcons;
    private int           maxInlineValues;
    private int           maxTileDownloadThreads;
    private int           notificationCacheSize;
    private int           autoLockDelay;
    private final boolean alwaysDrawBoundingBoxes;
    private final boolean jsConsoleEnabled;
    private final boolean hwAccelerationEnabled;
    private final int     connectedNodeTolerance;

    private static final String DEFAULT_MAP_PROFILE = "Color Round Nodes";

    private final SharedPreferences prefs;

    private final Resources r;

    /**
     * Construct a new instance
     * 
     * @param ctx Android context
     * @throws IllegalArgumentException
     * @throws NotFoundException
     */
    @SuppressLint("NewApi")
    public Preferences(@NonNull Context ctx) throws IllegalArgumentException, NotFoundException {
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        r = ctx.getResources();
        advancedPrefs = new AdvancedPrefDatabase(ctx);

        // we're not using acra.disable - ensure it isn't present
        if (prefs.contains("acra.disable")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("acra.disable");
            editor.commit();
        }
        // we *are* using acra.enable
        if (!prefs.contains("acra.enable")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("acra.enable", true);
            editor.commit();
        }

        maxStrokeWidth = getIntPref(R.string.config_maxStrokeWidth_key, 16);

        tileCacheSize = getIntPref(R.string.config_tileCacheSize_key, 100);

        downloadRadius = getIntPref(R.string.config_extTriggeredDownloadRadius_key, 50);
        maxDownloadSpeed = getIntPref(R.string.config_maxDownloadSpeed_key, 6);

        bugDownloadRadius = getIntPref(R.string.config_bugDownloadRadius_key, 200);
        maxBugDownloadSpeed = getIntPref(R.string.config_maxBugDownloadSpeed_key, 30);

        taskFilter = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            taskFilter = prefs.getStringSet(r.getString(R.string.config_bugFilter_key), taskFilter);
        }

        isStatsVisible = prefs.getBoolean(r.getString(R.string.config_showStats_key), false);
        isToleranceVisible = prefs.getBoolean(r.getString(R.string.config_showTolerance_key), true);
        isAntiAliasingEnabled = prefs.getBoolean(r.getString(R.string.config_enableAntiAliasing_key), true);
        isOpenStreetBugsEnabled = prefs.getBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), true);
        isPhotoLayerEnabled = prefs.getBoolean(r.getString(R.string.config_enablePhotoLayer_key), false);
        tagFormEnabled = prefs.getBoolean(r.getString(R.string.config_tagFormEnabled_key), true);
        isKeepScreenOnEnabled = prefs.getBoolean(r.getString(R.string.config_enableKeepScreenOn_key), false);
        useBackForUndo = prefs.getBoolean(r.getString(R.string.config_use_back_for_undo_key), false);
        largeDragArea = prefs.getBoolean(r.getString(R.string.config_largeDragArea_key), false);
        enableNameSuggestions = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestions_key), true);
        nameSuggestionPresetsEnabled = prefs.getBoolean(r.getString(R.string.config_enableNameSuggestionsPresets_key), true);
        closeChangesetOnSave = prefs.getBoolean(r.getString(R.string.config_closeChangesetOnSave_key), true);
        splitActionBarEnabled = prefs.getBoolean(r.getString(R.string.config_splitActionBarEnabled_key), true);
        backgroundLayer = prefs.getString(r.getString(R.string.config_backgroundLayer_key), "MAPNIK");
        overlayLayer = prefs.getString(r.getString(R.string.config_overlayLayer_key), "NOOVERLAY");
        scaleLayer = prefs.getString(r.getString(R.string.config_scale_key), "SCALE_METRIC");
        String tempMapProfile = prefs.getString(r.getString(R.string.config_mapProfile_key), null);
        // check if we actually still have the profile
        if (DataStyle.getStyle(tempMapProfile) == null) {
            if (DataStyle.getStyle(DEFAULT_MAP_PROFILE) == null) {
                Log.w(DEBUG_TAG, "Using builtin default profile instead of " + tempMapProfile + " and " + DEFAULT_MAP_PROFILE);
                mapProfile = DataStyle.getBuiltinStyleName(); // built-in fall back
            } else {
                Log.w(DEBUG_TAG, "Using default profile");
                mapProfile = DEFAULT_MAP_PROFILE;
            }
        } else {
            mapProfile = tempMapProfile;
        }
        gpsSource = prefs.getString(r.getString(R.string.config_gps_source_key), "internal");
        gpsTcpSource = prefs.getString(r.getString(R.string.config_gps_source_tcp_key), "127.0.0.1:1958");
        gpsDistance = getIntPref(R.string.config_gps_distance_key, 2);
        gpsInterval = getIntPref(R.string.config_gps_interval_key, 1000);

        forceContextMenu = prefs.getBoolean(r.getString(R.string.config_forceContextMenu_key), false);

        offsetServer = prefs.getString(r.getString(R.string.config_offsetServer_key), Urls.DEFAULT_OFFSET_SERVER);
        osmoseServer = prefs.getString(r.getString(R.string.config_osmoseServer_key), Urls.DEFAULT_OSMOSE_SERVER);
        mapRouletteServer = prefs.getString(r.getString(R.string.config_maprouletteServer_key), Urls.DEFAULT_MAPROULETTE_SERVER);
        taginfoServer = prefs.getString(r.getString(R.string.config_taginfoServer_key), Urls.DEFAULT_TAGINFO_SERVER);

        showCameraAction = prefs.getBoolean(r.getString(R.string.config_showCameraAction_key), true);
        useInternalPhotoViewer = prefs.getBoolean(r.getString(R.string.config_useInternalPhotoViewer_key), true);

        generateAlerts = prefs.getBoolean(r.getString(R.string.config_generateAlerts_key), false);
        maxAlertDistance = getIntPref(R.string.config_maxAlertDistance_key, 100);
        groupAlertsOnly = prefs.getBoolean(r.getString(R.string.config_groupAlertsOnly_key), false);

        // light theme now always default
        lightThemeEnabled = prefs.getBoolean(r.getString(R.string.config_enableLightTheme_key), true);

        addressTags = new HashSet<>(Arrays.asList(r.getStringArray(R.array.address_tags_defaults)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            addressTags = prefs.getStringSet(r.getString(R.string.config_addressTags_key), addressTags);
        }

        voiceCommandsEnabled = prefs.getBoolean(r.getString(R.string.config_voiceCommandsEnabled_key), false);

        leaveGpsDisabled = prefs.getBoolean(r.getString(R.string.config_leaveGpsDisabled_key), false);
        allowFallbackToNetworkLocation = prefs.getBoolean(r.getString(R.string.config_gps_network_key), true);

        showIcons = prefs.getBoolean(r.getString(R.string.config_showIcons_key), true);

        showWayIcons = prefs.getBoolean(r.getString(R.string.config_showWayIcons_key), true);

        followGPSbutton = prefs.getString(r.getString(R.string.config_followGPSbutton_key), r.getString(R.string.follow_GPS_left));

        fullscreenMode = prefs.getString(r.getString(R.string.config_fullscreenMode_key),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? r.getString(R.string.full_screen_auto) : r.getString(R.string.full_screen_never));

        mapOrientation = prefs.getString(r.getString(R.string.config_mapOrientation_key), r.getString(R.string.map_orientation_auto));

        maxInlineValues = getIntPref(R.string.config_maxInlineValues_key, 4);

        autoLockDelay = getIntPref(R.string.config_autoLockDelay_key, 60);

        notificationCacheSize = getIntPref(R.string.config_notificationCacheSize_key, 5);

        maxTileDownloadThreads = getIntPref(R.string.config_maxTileDownloadThreads_key, 4);

        alwaysDrawBoundingBoxes = prefs.getBoolean(r.getString(R.string.config_alwaysDrawBoundingBoxes_key), true);

        jsConsoleEnabled = prefs.getBoolean(r.getString(R.string.config_js_console_key), false);

        hwAccelerationEnabled = prefs.getBoolean(r.getString(R.string.config_enableHwAcceleration_key), false);

        connectedNodeTolerance = getIntPref(R.string.config_connectedNodeTolerance_key, 2);
    }

    /**
     * Get an integer valued preference
     * 
     * @param keyResId the res id
     * @param def default value
     * @return the stored preference or the default if none found
     */
    int getIntPref(int keyResId, int def) {
        String key = r.getString(keyResId);
        try {
            return prefs.getInt(r.getString(keyResId), def);
        } catch (ClassCastException e) {
            Log.w(DEBUG_TAG, "error retrieving pref for " + key);
            return def;
        }
    }

    /**
     * @return the maximum width of a stroke
     */
    public float getMaxStrokeWidth() {
        return maxStrokeWidth;
    }

    /**
     * @return the size of the tile cache in MB
     */
    public int getTileCacheSize() {
        return tileCacheSize;
    }

    /**
     * Check if some debugging stats should be shown on screen
     * 
     * @return true if turned on
     */
    public boolean isStatsVisible() {
        return isStatsVisible;
    }

    /**
     * Check if touch tolerance areas should be shown on screen
     * 
     * @return true if the areas should be shown
     */
    public boolean isToleranceVisible() {
        return isToleranceVisible;
    }

    /**
     * Check if anti-aliasing should be used
     * 
     * @return true if anto-aliasing should be used
     */
    public boolean isAntiAliasingEnabled() {
        return isAntiAliasingEnabled;
    }

    /**
     * Check if the task layer is anabled
     * 
     * @return true if enabled
     */
    public boolean areBugsEnabled() {
        return isOpenStreetBugsEnabled;
    }

    /**
     * Set the enabled status of the tasks/bugs layer
     * 
     * @param on if true enable layer
     */
    public void setBugsEnabled(boolean on) {
        isOpenStreetBugsEnabled = on;
        prefs.edit().putBoolean(r.getString(R.string.config_enableOpenStreetBugs_key), on).commit();
    }

    /**
     * Check if the photo layer is enabled
     * 
     * @return true if enabled
     */
    public boolean isPhotoLayerEnabled() {
        return isPhotoLayerEnabled;
    }

    /**
     * Set the enabled status of the photo layer
     * 
     * @param enabled if true enable layer
     */
    public void setPhotoLayerEnabled(boolean enabled) {
        isPhotoLayerEnabled = enabled;
        prefs.edit().putBoolean(r.getString(R.string.config_enablePhotoLayer_key), enabled).commit();
    }

    /**
     * Check if the form based tag editor should be shown in the property editor
     * 
     * @return true if the form based editor should be used
     */
    public boolean tagFormEnabled() {
        return tagFormEnabled;
    }

    /**
     * Check if we want to keep the screen on
     * 
     * @return true if the screen should be kept on
     */
    public boolean isKeepScreenOnEnabled() {
        return isKeepScreenOnEnabled;
    }

    /**
     * Check if the back key should be used for undo
     * 
     * @return true if the back key should be used for undo
     */
    public boolean useBackForUndo() {
        return useBackForUndo;
    }

    /**
     * Check if we should show a large drag area around nodes
     * 
     * @return true if we should show a large drag area
     */
    public boolean largeDragArea() {
        return largeDragArea;
    }

    /**
     * @return the id of the current background layer
     */
    public String backgroundLayer() {
        return backgroundLayer;
    }

    /**
     * Set the id of the background layer
     * 
     * @param id id to set
     */
    public void setBackGroundLayer(String id) {
        backgroundLayer = id;
        prefs.edit().putString(r.getString(R.string.config_backgroundLayer_key), id).commit();
    }

    /**
     * @return the id of the current overlay layer
     */
    public String overlayLayer() {
        return overlayLayer;
    }

    /**
     * Set the id of the overlay layer
     * 
     * @param id id to set
     */
    public void setOverlayLayer(String id) {
        overlayLayer = id;
        prefs.edit().putString(r.getString(R.string.config_overlayLayer_key), id).commit();
    }

    /**
     * Get kind of scale that should be displayed
     * 
     * @return mode value from scale_values
     */
    public String scaleLayer() {
        return scaleLayer;
    }

    /**
     * Set the kind of scale that should be displayed
     * 
     * @param mode value from scale_values
     */
    public void setScaleLayer(String mode) {
        scaleLayer = mode;
        prefs.edit().putString(r.getString(R.string.config_scale_key), mode).commit();
    }

    /**
     * Get the current data rendering style
     * 
     * @return the name of the current data renderign style
     */
    @NonNull
    public String getMapProfile() {
        return mapProfile;
    }

    /**
     * Get the currently used API server
     * 
     * @return the current Server object
     */
    @NonNull
    public Server getServer() {
        return advancedPrefs.getServerObject();
    }

    /**
     * Get the name of the current API configuration
     * 
     * @return the name or null if not found
     */
    @Nullable
    public String getApiName() {
        API api = advancedPrefs.getCurrentAPI();
        return api != null ? api.name : null;
    }

    /**
     * Get the currently active Presets
     * 
     * @return an array holding the Presets
     */
    @Nullable
    public Preset[] getPreset() {
        return advancedPrefs.getCurrentPresetObject();
    }

    /**
     * Check if we should show icons on nodes
     * 
     * @return true if icons should be shown
     */
    public boolean getShowIcons() {
        return showIcons;
    }

    /**
     * Check if we should show icons on closed ways
     * 
     * @return true if icons should be shown
     */
    public boolean getShowWayIcons() {
        return showWayIcons;
    }

    /**
     * Get the current GPS/GNSS source
     * 
     * @return a String with the source name
     */
    public String getGpsSource() {
        return gpsSource;
    }

    /**
     * Get the current GPS/GNSS tcp source
     * 
     * @return a String with the source name
     */
    public String getGpsTcpSource() {
        return gpsTcpSource;
    }

    /**
     * Get the configured minimum time between GPS/GNSS location fixes
     * 
     * @return interval between GPS/GNSS location fixes in miliseconds
     */
    public int getGpsInterval() {
        return gpsInterval;
    }

    /**
     * Get the configured minimum distance between GPS/GNSS location fixes
     * 
     * @return distance between GPS/GNSS location fixes in meters
     */
    public float getGpsDistance() {
        return gpsDistance;
    }

    /**
     * Check if we are allowed to fall back to Network locations
     * 
     * @return true if the fallback is allowed
     */
    public boolean isNetworkLocationFallbackAllowed() {
        return allowFallbackToNetworkLocation;
    }

    /**
     * Always show the selection context menu if more than one element is in the click tolerance
     * 
     * @return true if we should always show the context menu
     */
    public boolean getForceContextMenu() {
        return forceContextMenu;
    }

    /**
     * Check if we should use the name suggestion index
     * 
     * @return true if we should use the index
     */
    public boolean getEnableNameSuggestions() {
        return enableNameSuggestions;
    }

    /**
     * Get the configured download radius for data
     * 
     * @return the radius in meters
     */
    public int getDownloadRadius() {
        return downloadRadius;
    }

    /**
     * Get the configured maximum speed up to which we still auto-download data
     * 
     * @return the maximum speed for autodownloads
     */
    public float getMaxDownloadSpeed() {
        return maxDownloadSpeed;
    }

    /**
     * Set maximum speed for autodownloads
     * 
     * @param maxDownloadSpeed max speed in km/h to set
     */
    public void setMaxDownloadSpeed(float maxDownloadSpeed) {
        this.maxDownloadSpeed = maxDownloadSpeed;
        prefs.edit().putInt(r.getString(R.string.config_maxDownloadSpeed_key), (int) maxDownloadSpeed).commit();
    }

    /**
     * Get the configured download radius for tasks
     * 
     * @return the radius in meters
     */
    public int getBugDownloadRadius() {
        return bugDownloadRadius;
    }

    /**
     * Get the configured maximum speed up to which we still auto-download tasks
     * 
     * @return the maximum speed for autodownloads
     */
    public float getMaxBugDownloadSpeed() {
        return maxBugDownloadSpeed;
    }

    /**
     * Get the currently enabled task types
     * 
     * @return a set containing the task types as strings
     */
    public Set<String> taskFilter() {
        return taskFilter;
    }

    /**
     * Set the task filter
     * 
     * @param tasks the tasks to use, if null the default setting will be set (all on)
     */
    public void setTaskFilter(@Nullable Set<String> tasks) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (tasks == null) {
                tasks = new HashSet<>(Arrays.asList(r.getStringArray(R.array.bug_filter_defaults)));
            }
            taskFilter = tasks;
            prefs.edit().putStringSet(r.getString(R.string.config_bugFilter_key), tasks).commit();
        }
    }

    /**
     * Is automatically applying presets for name suggestions turned on
     * 
     * @return true if automatically applying presets for name suggestions should be used
     */
    public boolean nameSuggestionPresetsEnabled() {
        //
        return nameSuggestionPresetsEnabled;
    }

    /**
     * Check how we should handle changesets
     * 
     * @return true if we should close changesets on save
     */
    public boolean closeChangesetOnSave() {
        return closeChangesetOnSave;
    }

    /**
     * Check if we should split the action bar
     * 
     * @return true if we should show the action bar at top and bottom of the screen
     */
    public boolean splitActionBarEnabled() {
        return splitActionBarEnabled;
    }

    /**
     * Get the configured offset database server
     * 
     * @return base url for the server
     */
    public String getOffsetServer() {
        return offsetServer;
    }

    /**
     * Get the configured OSMOSE server
     * 
     * @return base url for the server
     */
    public String getOsmoseServer() {
        return osmoseServer;
    }

    /**
     * Get the configured MapRoulette server
     * 
     * @return base url for the server
     */
    public String getMapRouletteServer() {
        return mapRouletteServer;
    }

    /**
     * Get the configured taginfo server
     * 
     * @return base url for the server
     */
    public String getTaginfoServer() {
        return taginfoServer;
    }

    /**
     * set the configured taginfo server
     * 
     * @param url base url for the server
     */
    public void setTaginfoServer(String url) {
        this.taginfoServer = url;
        prefs.edit().putString(r.getString(R.string.config_taginfoServer_key), url).commit();
    }

    /**
     * Check if we should show a camera button on the main map screen
     * 
     * @return true if the camera button should be shown
     */
    public boolean showCameraAction() {
        return showCameraAction;
    }

    /**
     * Check if we should use the internal photo viewer
     * 
     * @return true if the internal photo viewer should be used
     */
    public boolean useInternalPhotoViewer() {
        return useInternalPhotoViewer;
    }

    /**
     * Check if we should generate alerts (Android Notifications)
     * 
     * @return true if we should generate alerts
     */
    public boolean generateAlerts() {
        return generateAlerts;
    }

    /**
     * Check if we should generate alerts only once per group or always
     * 
     * @return true if we should generate alerts once per group or always
     */
    public boolean groupAlertOnly() {
        return groupAlertsOnly;
    }

    /**
     * Check if the light theme is enabled
     * 
     * @return true if the light theme is enabled
     */
    public boolean lightThemeEnabled() {
        return lightThemeEnabled;
    }

    /**
     * Get the address tags we currently want to use
     * 
     * @return a Set containing the tags
     */
    public Set<String> addressTags() {
        return addressTags;
    }

    public int getMaxAlertDistance() {
        return maxAlertDistance;
    }

    public boolean voiceCommandsEnabled() {
        return voiceCommandsEnabled;
    }

    /**
     * Check if we should attempt to turn GPS on is disabled
     * 
     * @return true if we shouldn't try
     */
    public boolean leaveGpsDisabled() {
        return leaveGpsDisabled;
    }

    /**
     * Get the position of the on screen follow location button
     * 
     * @return one of LEFT, RIGHT and NONE
     */
    public String followGPSbuttonPosition() {
        return followGPSbutton;
    }

    /**
     * Get the fullscreen mode
     * 
     * @return one of AUTO, NEVER, FORCE and NOSTATUSBAR
     */
    public String getFullscreenMode() {
        return fullscreenMode;
    }

    /**
     * Get the way the map screen should respond to orientation changes
     * 
     * @return one of AUTO, CURRENT, PORTRAIT and LANDSCAPE
     */
    public String getMapOrientation() {
        return mapOrientation;
    }

    /**
     * Get the max number of items that will be displayed inline in the tag form editor
     * 
     * @return the max number on inline values displayed
     */
    public int getMaxInlineValues() {
        return maxInlineValues;
    }

    public int getMaxTileDownloadThreads() {
        return maxTileDownloadThreads;
    }

    public int getNotificationCacheSize() {
        if (notificationCacheSize < 1) {
            Log.e(DEBUG_TAG, "Notification cache size smaller than 1");
            return 1;
        }
        return notificationCacheSize;
    }

    /**
     * Get the number of seconds we should wait before locking the display
     * 
     * @return delay in seconds till we auto-lock
     */
    public int getAutolockDelay() {
        return 1000 * autoLockDelay;
    }

    public void setAutoDownload(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_autoDownload_key), on).commit();
    }

    public boolean getAutoDownload() {
        String key = r.getString(R.string.config_autoDownload_key);
        if (!prefs.contains(key)) {
            // create the entry
            setAutoDownload(false);
        }
        return prefs.getBoolean(key, false);
    }

    public void setContrastValue(float cValue) {
        prefs.edit().putFloat(r.getString(R.string.config_contrastValue_key), cValue).commit();
    }

    public float getContrastValue() {
        String key = r.getString(R.string.config_contrastValue_key);
        if (!prefs.contains(key)) {
            // create the entry
            setContrastValue(0);
        }
        return prefs.getFloat(key, 0);
    }

    public void setBugAutoDownload(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_bugAutoDownload_key), on).commit();
    }

    public boolean getBugAutoDownload() {
        String key = r.getString(R.string.config_bugAutoDownload_key);
        if (!prefs.contains(key)) {
            // create the entry
            setBugAutoDownload(false);
        }
        return prefs.getBoolean(key, false);
    }

    public void setShowGPS(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_showGPS_key), on).commit();
    }

    public boolean getShowGPS() {
        String key = r.getString(R.string.config_showGPS_key);
        if (!prefs.contains(key)) {
            // create the entry
            setShowGPS(true);
        }
        return prefs.getBoolean(key, true);
    }

    public boolean getAlwaysDrawBoundingBoxes() {
        return alwaysDrawBoundingBoxes;
    }

    public void enableTagFilter(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_tagFilter_key), on).commit();
    }

    public boolean getEnableTagFilter() {
        String key = r.getString(R.string.config_tagFilter_key);
        if (!prefs.contains(key)) {
            // create the entry
            enableTagFilter(false);
        }
        return prefs.getBoolean(key, false);
    }

    public void enablePresetFilter(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_presetFilter_key), on).commit();
    }

    public boolean getEnablePresetFilter() {
        String key = r.getString(R.string.config_presetFilter_key);
        if (!prefs.contains(key)) {
            // create the entry
            enablePresetFilter(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Set the current default Geocoder
     * 
     * @param index index of the Geocoder
     */
    public void setGeocoder(int index) {
        prefs.edit().putInt(r.getString(R.string.config_geocoder_key), index).commit();
    }

    /**
     * Get the current default Geocoder
     * 
     * @return index of the Geocoder
     */
    public int getGeocoder() {
        String key = r.getString(R.string.config_geocoder_key);
        if (!prefs.contains(key)) {
            // create the entry
            setGeocoder(0);
        }
        return prefs.getInt(key, 0);
    }

    /**
     * Set if searches should be limited to the current ViewBox
     * 
     * @param on if true limit searches to the current ViewBox
     */
    public void setGeocoderLimit(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_geocoderLimit_key), on).commit();
    }

    /**
     * Check if we should limit searches to the current ViewBox
     * 
     * @return true if searches should be limited
     */
    public boolean getGeocoderLimit() {
        String key = r.getString(R.string.config_geocoderLimit_key);
        if (!prefs.contains(key)) {
            // create the entry
            setGeocoderLimit(false);
        }
        return prefs.getBoolean(key, false);
    }

    public boolean isJsConsoleEnabled() {
        return jsConsoleEnabled;
    }

    public boolean hwAccelerationEnabled() {
        return hwAccelerationEnabled;
    }

    /**
     * Enable/disable the simple actions
     * 
     * @param on value to set
     */
    public void enableSimpleActions(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_simpleActions_key), on).commit();
    }

    /**
     * Check if the simply actions are enabled
     * 
     * @return true if enabled
     */
    public boolean areSimpleActionsEnabled() {
        String key = r.getString(R.string.config_simpleActions_key);
        if (!prefs.contains(key)) {
            // create the entry
            enableSimpleActions(true);
        }
        return prefs.getBoolean(key, true);
    }

    /**
     * If (highway) end nodes are less than this difference away from a way they could connect to fail validation
     * 
     * @return the tolerance in meters
     */
    public int getConnectedNodeTolerance() {
        return connectedNodeTolerance;
    }

    /**
     * Allow / disallow all networks for downloads
     * 
     * @param on value to set
     */
    public void setAllowAllNetworks(boolean on) {
        prefs.edit().putBoolean(r.getString(R.string.config_allowAllNetworks_key), on).commit();
    }

    /**
     * Check if all networks are allowed for downloads
     * 
     * @return true if enabled
     */
    public boolean allowAllNetworks() {
        String key = r.getString(R.string.config_allowAllNetworks_key);
        if (!prefs.contains(key)) {
            // create the entry
            setAllowAllNetworks(false);
        }
        return prefs.getBoolean(key, false);
    }

    /**
     * Get a string from shared preferences
     * 
     * @param prefKey preference key as a string resource
     * @return the strings or null if nothing was found
     */
    @Nullable
    public String getString(int prefKey) {
        try {
            String key = r.getString(prefKey);
            return prefs.getString(key, null);
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "getString " + ex.getMessage());
            return null;
        }
    }

    /**
     * Save a string to shared preferences
     * 
     * @param prefKey preference key as a string resource
     * @param s string value to save
     */
    public void putString(int prefKey, @NonNull String s) {
        try {
            String key = r.getString(prefKey);
            if (key != null) {
                prefs.edit().putString(key, s).commit();
            } else {
                Log.e(DEBUG_TAG, Integer.toString(prefKey) + " is not a valid string resource");
            }
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "putString " + ex.getMessage());
        }
    }
}
