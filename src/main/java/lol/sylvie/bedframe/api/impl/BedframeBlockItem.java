package lol.sylvie.bedframe.api.impl;

import eu.pb4.polymer.core.api.item.PolymerBlockItem;
import lol.sylvie.bedframe.api.BedframeItem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class BedframeBlockItem extends PolymerBlockItem implements BedframeItem {
    public BedframeBlockItem(Block block, Settings settings) {
        super(block, settings, Items.BARRIER, true);
    }

    @Override
    public Item getItem() {
        return this;
    }
}
