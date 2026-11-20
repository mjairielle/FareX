package com.farex.ui.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import javafx.scene.image.Image;

import com.gluonhq.maps.tile.TileRetriever;

// Street tiles for RouteMap: CARTO's "Voyager" style (OpenStreetMap data, Google-like colours).
// Gluon Maps finds this class through META-INF/services/com.gluonhq.maps.tile.TileRetriever.
//
// CARTO needs an API key: without one every tile is a grey "API KEY REQUIRED" picture. The key is
// read from (first found wins):
//   1. the system property farex.cartoKey      (java -Dfarex.cartoKey=... on a laptop)
//   2. the environment variable CARTO_API_KEY
//   3. the file resources/com/farex/ui/common/carto.key (one line; bundled into the APK)
// carto.key is in .gitignore so the key never lands on GitHub. Anyone with the APK can still dig
// it out, as with every map key in a phone app: limit it in the CARTO dashboard if that matters.
//
// No key -> plain OpenStreetMap tiles. OSM blocks Java's default "Java/21" user agent ("Access
// blocked"), so the app names itself in http.agent, which Java's URL loading (and so Image) sends.
// shortcut: no disk cache (tiles download again each run), add one if mobile data use matters.
public class VoyagerTiles implements TileRetriever {

    private static final String[] HOSTS = {"a", "b", "c", "d"};
    private static final String KEY = findKey();

    /** What the map must credit for the tiles in use. */
    public static final String CREDIT = KEY != null ? "© OpenStreetMap contributors © CARTO" : "© OpenStreetMap contributors";

    static {
        if (System.getProperty("http.agent") == null) {
            System.setProperty("http.agent", "FareX/1.0 (Quezon City jeepney fare app)");
        }
        System.err.println(KEY != null ? "Map tiles: CARTO Voyager (API key found)"
                                       : "Map tiles: OpenStreetMap (no CARTO key: see VoyagerTiles)");
    }

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long i, long j) {
        String url = KEY != null
                ? "https://" + HOSTS[(int) Math.floorMod(i + j, 4L)] + ".basemaps.cartocdn.com/rastertiles/voyager/"
                        + zoom + "/" + i + "/" + j + ".png?key=" + URLEncoder.encode(KEY, StandardCharsets.UTF_8)
                : "https://tile.openstreetmap.org/" + zoom + "/" + i + "/" + j + ".png";
        return CompletableFuture.completedFuture(new Image(url, true));
    }

    private static String findKey() {
        String key = System.getProperty("farex.cartoKey");
        if (blank(key)) key = System.getenv("CARTO_API_KEY");
        if (blank(key)) {
            try (InputStream in = VoyagerTiles.class.getResourceAsStream("carto.key")) {
                if (in != null) key = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException unreadable) {
                key = null;
            }
        }
        // strip a BOM, spaces and line breaks a text editor may add
        return blank(key) ? null : key.replace("﻿", "").strip();
    }

    private static boolean blank(String s) {
        return s == null || s.replace("﻿", "").isBlank();
    }
}
