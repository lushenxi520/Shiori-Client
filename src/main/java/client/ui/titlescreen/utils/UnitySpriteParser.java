package client.ui.titlescreen.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.humbleui.skija.Image;
import io.github.humbleui.types.Rect;
import io.github.humbleui.skija.Surface;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class UnitySpriteParser {

    private static final Gson GSON = new Gson();

    public static class SpriteData {
        public final String name;
        public final float x;
        public final float y;
        public final float width;
        public final float height;

        public SpriteData(String name, float x, float y, float width, float height) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static class SpriteAtlas {
        public final String name;
        public final Map<String, SpriteData> sprites;

        public SpriteAtlas(String name, Map<String, SpriteData> sprites) {
            this.name = name;
            this.sprites = sprites;
        }
    }

    public static SpriteAtlas parseAtlas(InputStream inputStream) {
        try {
            String json = new String(inputStream.readAllBytes());
            inputStream.close();
            return parseAtlas(json);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SpriteAtlas parseAtlas(String json) {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
        String atlasName = jsonObject.get("m_Name").getAsString();

        Map<String, SpriteData> sprites = new HashMap<>();
        var namesArray = jsonObject.getAsJsonArray("m_PackedSpriteNamesToIndex");
        var renderDataArray = jsonObject.getAsJsonArray("m_RenderDataMap");

        for (int i = 0; i < renderDataArray.size(); i++) {
            String name = namesArray.get(i).getAsString();
            var rect = renderDataArray.get(i).getAsJsonObject()
                    .getAsJsonObject("Value")
                    .getAsJsonObject("m_TextureRect");

            sprites.put(name, new SpriteData(
                    name,
                    rect.get("m_X").getAsFloat(),
                    rect.get("m_Y").getAsFloat(),
                    rect.get("m_Width").getAsFloat(),
                    rect.get("m_Height").getAsFloat()
            ));
        }

        return new SpriteAtlas(atlasName, sprites);
    }

    public static Image cropSprite(Image atlas, SpriteData sprite) {
        return cropSprite(atlas, sprite, atlas.getHeight());
    }

    public static Image cropSprite(Image atlas, SpriteData sprite, int atlasHeight) {
        float skiaY = atlasHeight - sprite.y - sprite.height;
        int width = (int) sprite.width;
        int height = (int) sprite.height;

        Surface surface = Surface.makeRasterN32Premul(width, height);
        surface.getCanvas().drawImageRect(
                atlas,
                Rect.makeXYWH(sprite.x, skiaY, sprite.width, sprite.height),
                Rect.makeWH(sprite.width, sprite.height)
        );
        Image result = surface.makeImageSnapshot();
        surface.close();
        return result;
    }

    public static SpriteAtlas loadAtlasDataFromResources(String jsonPath) {
        try (InputStream is = UnitySpriteParser.class.getResourceAsStream(jsonPath)) {
            if (is == null) return null;
            return parseAtlas(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Image loadAtlasImageFromResources(String imagePath) {
        try (InputStream is = UnitySpriteParser.class.getResourceAsStream(imagePath)) {
            if (is == null) return null;
            return Image.makeFromEncoded(is.readAllBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}