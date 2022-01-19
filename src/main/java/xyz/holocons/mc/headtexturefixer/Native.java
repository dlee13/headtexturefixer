package xyz.holocons.mc.headtexturefixer;

public final class Native {

    public static final String LIBRARY_NAME = System.mapLibraryName("headtexturefixer_native");
    
    public static native String normalizeTexture(String base64);
}
