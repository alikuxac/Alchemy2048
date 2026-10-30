package com.alikuxac.alchemy2048.core;

public enum Direction2048 {
    UP((byte) 0),
    DOWN((byte) 1),
    LEFT((byte) 2),
    RIGHT((byte) 3);

    private final byte id;

    Direction2048(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static Direction2048 fromId(byte id) {
        return switch (id) {
            case 0 -> UP;
            case 1 -> DOWN;
            case 2 -> LEFT;
            case 3 -> RIGHT;
            default -> null;
        };
    }
}
