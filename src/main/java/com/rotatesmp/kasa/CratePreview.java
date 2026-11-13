package com.rotatesmp.kasa;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Kasa icerigini salt okunur gosteren onizleme envanteri. */
public final class CratePreview implements InventoryHolder {

    private final Inventory inventory;

    public CratePreview(ItemStack[] contents) {
        this.inventory = Bukkit.createInventory(this, 27, CrateManager.MM.deserialize("<gold><bold>Kasa Önizleme"));
        for (int i = 0; i < Math.min(27, contents.length); i++) {
            if (contents[i] != null) {
                inventory.setItem(i, contents[i].clone());
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
