package lol.sylvie.bedframe.api.impl;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import lol.sylvie.bedframe.api.BedframeItem;
import net.minecraft.item.Item;

public class SimpleBedframeItem extends SimplePolymerItem implements BedframeItem {
    public SimpleBedframeItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getItem() {
        return this;
    }
}
