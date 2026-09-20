package xyz.stormclient.bridge;

/** The packets Storm actually cares about. Everything else maps to OTHER. */
public enum PacketType {
    // client -> server
    C_PLAYER,               // onGround only
    C_PLAYER_POSITION,
    C_PLAYER_LOOK,
    C_PLAYER_POSITION_LOOK,
    C_USE_ENTITY,
    C_ANIMATION,
    C_ENTITY_ACTION,
    C_BLOCK_PLACE,
    C_BLOCK_DIG,
    C_HELD_ITEM_CHANGE,
    C_CHAT,
    C_KEEP_ALIVE,
    C_WINDOW_CLICK,
    C_CLOSE_WINDOW,
    C_CONFIRM_TRANSACTION,

    // server -> client
    S_PLAYER_POS_LOOK,
    S_ENTITY_VELOCITY,
    S_EXPLOSION,
    S_ENTITY_STATUS,
    S_CHAT,
    S_TITLE,
    S_KEEP_ALIVE,
    S_DISCONNECT,
    S_SPAWN_PLAYER,
    S_SPAWN_ENTITY,
    S_DESTROY_ENTITIES,
    S_UPDATE_HEALTH,
    S_OPEN_WINDOW,
    S_WINDOW_ITEMS,
    S_CONFIRM_TRANSACTION,
    S_BLOCK_CHANGE,
    S_TELEPORT,
    S_RESPAWN,

    OTHER
}
