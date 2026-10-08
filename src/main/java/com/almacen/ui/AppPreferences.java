package com.almacen.ui;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Preferencias de la aplicación. El usuario solo configura una carpeta de información:
 * ahí viven las bases de datos (.db) y, junto a cada una, su carpeta de fotos con el
 * mismo nombre seguido de "Fotos" (inventario.db → inventarioFotos).
 */
public final class AppPreferences {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppPreferences.class);
    private static final String KEY_DB_FOLDER = "db_folder_path";
    private static final String KEY_PHOTO_FOLDER = "photo_folder_path";
    private static final String KEY_LAST_DB = "last_db_name";
    /** Carpeta de fotos usada antes de que cada base tuviera la suya; se consulta al buscar fotos antiguas. */
    private static final String KEY_LEGACY_PHOTO_FOLDER = "legacy_photo_folder_path";
    private static final String SUFIJO_FOTOS = "Fotos";
    private static final String KEY_MAYUSCULAS = "escribir_mayusculas";

    private AppPreferences() {
    }

    /** Carpeta global donde se guarda toda la información. */
    public static void setDbFolderPath(String path) {
        if (path == null) {
            return;
        }
        PREFS.put(KEY_DB_FOLDER, path);
    }

    public static String getDbFolderPath() {
        return PREFS.get(KEY_DB_FOLDER, "");
    }

    public static String getUltimaBaseDatos() {
        return PREFS.get(KEY_LAST_DB, "");
    }

    /**
     * Registra la base de datos en uso: la carpeta de fotos pasa a ser la de esa base
     * (se crea si no existe) y se recuerda para la próxima vez.
     */
    public static File usarBaseDatos(File carpeta, String nombreBaseDatos) {
        String anterior = PREFS.get(KEY_PHOTO_FOLDER, "");
        if (!anterior.isEmpty() && PREFS.get(KEY_LEGACY_PHOTO_FOLDER, "").isEmpty()
                && PREFS.get(KEY_LAST_DB, "").isEmpty()) {
            // Primera vez con el esquema nuevo: las fotos ya tomadas se siguen encontrando
            PREFS.put(KEY_LEGACY_PHOTO_FOLDER, anterior);
        }
        File fotos = carpetaFotos(carpeta, nombreBaseDatos);
        fotos.mkdirs();
        setDbFolderPath(carpeta.getAbsolutePath());
        PREFS.put(KEY_LAST_DB, nombreBaseDatos);
        PREFS.put(KEY_PHOTO_FOLDER, fotos.getAbsolutePath());
        return fotos;
    }

    /** Carpeta de fotos de una base de datos: mismo nombre sin extensión + "Fotos". */
    public static File carpetaFotos(File carpeta, String nombreBaseDatos) {
        String base = nombreBaseDatos;
        int punto = base.lastIndexOf('.');
        if (punto > 0) {
            base = base.substring(0, punto);
        }
        return new File(carpeta, base + SUFIJO_FOTOS);
    }

    /** Carpeta de fotos de la base de datos en uso. */
    public static String getPhotoFolderPath() {
        return PREFS.get(KEY_PHOTO_FOLDER, "");
    }

    /** Solo para pruebas o configuraciones manuales. */
    public static void setPhotoFolderPath(String path) {
        if (path == null) {
            return;
        }
        PREFS.put(KEY_PHOTO_FOLDER, path);
    }

    /**
     * Archivo de una foto guardada: se busca en la carpeta de la base en uso y, si no está,
     * en la carpeta de fotos que se usaba antes.
     */
    public static File archivoFoto(String nombreFoto) {
        File actual = new File(getPhotoFolderPath(), nombreFoto);
        if (actual.exists()) {
            return actual;
        }
        String legado = PREFS.get(KEY_LEGACY_PHOTO_FOLDER, "");
        if (!legado.isEmpty()) {
            File antiguo = new File(legado, nombreFoto);
            if (antiguo.exists()) {
                return antiguo;
            }
        }
        return actual;
    }

    /** Si está activo, todo lo que se escribe en los campos queda en MAYÚSCULAS (activo por omisión). */
    public static boolean isMayusculas() {
        return PREFS.getBoolean(KEY_MAYUSCULAS, true);
    }

    public static void setMayusculas(boolean activo) {
        PREFS.putBoolean(KEY_MAYUSCULAS, activo);
    }
}
