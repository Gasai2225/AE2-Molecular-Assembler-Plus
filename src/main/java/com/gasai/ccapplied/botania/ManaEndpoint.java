package com.gasai.ccapplied.botania;

/** Exact accepted amounts, in native Botania mana units. Never simulates by mutating. */
public interface ManaEndpoint {
    long stored();
    long space();
    long extract(long amount);
    long insert(long amount);
}
