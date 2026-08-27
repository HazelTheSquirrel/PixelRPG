package de.pixelrpg.rpg.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemServiceNormalizationTest {
    @Test
    void normalizeAddsPixelRpgNamespace() throws Exception {
        var method = ItemService.class.getDeclaredMethod("normalize", String.class);
        method.setAccessible(true);
        assertEquals("pixelrpg:example/item", method.invoke(null, "example/item"));
        assertEquals("pixelrpg:example/item", method.invoke(null, "PIXELRPG:Example/Item"));
    }
}
