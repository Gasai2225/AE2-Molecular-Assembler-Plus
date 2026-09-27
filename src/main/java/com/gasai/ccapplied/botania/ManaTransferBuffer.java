package com.gasai.ccapplied.botania;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;

/** Owns mana between two APIs, including when a simulated operation is later refused. */
public final class ManaTransferBuffer {
    public static final long CAPACITY = 1_000_000_000L;
    private long amount;

    public long amount() { return amount; }
    public void restore(long stored) {
        if (stored < 0 || stored > CAPACITY) throw new IllegalArgumentException("Invalid mana buffer: " + stored);
        amount = stored;
    }

    public long deposit(long offered) {
        long accepted = Math.min(Math.max(0, offered), CAPACITY - amount);
        amount += accepted;
        return accepted;
    }

    public long flushTo(MEStorage network, IActionSource action, long limit) {
        long moved = network.insert(ManaKey.INSTANCE, Math.min(amount, Math.max(0, limit)), Actionable.MODULATE, action);
        amount -= moved;
        return moved;
    }

    public long importTo(ManaEndpoint source, MEStorage network, IActionSource action, long limit) {
        long budget = Math.min(Math.max(0, limit), CAPACITY);
        long moved = network.insert(ManaKey.INSTANCE, Math.min(amount, budget), Actionable.MODULATE, action);
        amount -= moved;
        budget -= moved;
        if (amount != 0 || budget <= 0) return moved;

        long requested = Math.min(budget, Math.max(0, source.stored()));
        requested = network.insert(ManaKey.INSTANCE, requested, Actionable.SIMULATE, action);
        amount += source.extract(requested);
        long delivered = network.insert(ManaKey.INSTANCE, amount, Actionable.MODULATE, action);
        amount -= delivered;
        return moved + delivered;
    }

    public long exportTo(MEStorage network, ManaEndpoint target, IActionSource action, long limit) {
        long budget = Math.min(Math.max(0, limit), CAPACITY);
        long moved = target.insert(Math.min(amount, budget));
        amount -= moved;
        budget -= moved;
        if (amount != 0 || budget <= 0) return moved;

        long requested = Math.min(budget, Math.max(0, target.space()));
        amount += network.extract(ManaKey.INSTANCE, requested, Actionable.MODULATE, action);
        long delivered = target.insert(amount);
        amount -= delivered;
        return moved + delivered;
    }
}
