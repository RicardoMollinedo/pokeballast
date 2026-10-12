package com.ballastlane.pokeproject.bl.model;

public enum Role {
    /** Registered user: can import, re-sync and edit pokemon. */
    TRAINER,
    /** Can additionally delete local pokemon. Only created by the seed, never by self-registration. */
    ADMIN
}
