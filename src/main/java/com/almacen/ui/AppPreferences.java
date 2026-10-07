package com.almacen.ui;

import java.util.prefs.Preferences;

public final class AppPreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppPreferences.class);
    private static final String KEY_DB_FOLDER = "db_folder_path";
    private static final String KEY_PHOTO_FOLDER = "photo_folder_path";

    private AppPreferences() {
    }

    public static void setDbFolderPath(String path) {
        if (path == null) {
            return;
        }
        PREFS.put(KEY_DB_FOLDER, path);
    }

    public static String getDbFolderPath() {
        return PREFS.get(KEY_DB_FOLDER, "");
    }

    public static void setPhotoFolderPath(String path) {
        if (path == null) {
            return;
        }
        PREFS.put(KEY_PHOTO_FOLDER, path);
    }

    public static String getPhotoFolderPath() {
        return PREFS.get(KEY_PHOTO_FOLDER, "");
    }
}
