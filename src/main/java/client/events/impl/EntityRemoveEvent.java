package client.events.impl;

import client.events.api.events.Event;
import net.minecraft.world.entity.Entity;

public class EntityRemoveEvent implements Event {
    private final boolean dead;
    private final Entity entity;

    public EntityRemoveEvent(boolean dead, Entity entity) {
        this.dead = dead;
        this.entity = entity;
    }

    public boolean dead() {
        return this.dead;
    }

    public Entity entity() {
        return this.entity;
    }
}