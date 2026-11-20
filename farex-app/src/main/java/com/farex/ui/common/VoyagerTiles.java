package com.farex.ui.common;

import java.util.concurrent.CompletableFuture;

import javafx.scene.image.Image;

import com.gluonhq.maps.tile.TileRetriever;

// Street tiles for RouteMap: CARTO's "Voyager" style (OpenStreetMap data, Google-like colours).
// Gluon Maps finds this class through META-INF/services/com.gluonhq.maps.tile.TileRetriever;
// without that file it falls back to the plain OpenStreetMap tiles.
// shortcut: no disk cache (tiles download again each run), add one if mobile data use matters.
public class VoyagerTiles implements TileRetriever {

    private static final String[] HOSTS = {"a", "b", "c", "d"};

    @Override
    public CompletableFuture<Image> loadTile(int zoom, long i, long j) {
        String host = HOSTS[(int) Math.floorMod(i + j, 4L)];
        return CompletableFuture.completedFuture(new Image(
                "https://" + host + ".basemaps.cartocdn.com/rastertiles/voyager/" + zoom + "/" + i + "/" + j + ".png", true));
    }
}
