package com.tccec.emotionpimobile.game;

/**
 * Contains all actions processed by the Raspberry Pi server
 *
 * Created by Vinicius on 29/10/2016.
 */
public enum Action {
    INIT(1),
    ASKFOREMOTION(2),
    CANCELEMOTION(3),
    CONFIRMHITORMISS(4),
    LISTPLAYERS(5),
    PROCESSIMAGE(6),
    CONTINUEPROCESSIMAGE(7),
    STOPPROCESSIMAGE(8),
    REMOVEPLAYER(9);

    private Integer idValue;

    private Action(Integer id) {
        idValue = id;
    }

    public Integer getIntegerValue() {
        return idValue;
    }

    public static Action fromInteger(int x) {
        switch(x) {
            case 1:
                return INIT;
            case 2:
                return ASKFOREMOTION;
            case 3:
                return CANCELEMOTION;
            case 4:
                return CONFIRMHITORMISS;
            case 5:
                return LISTPLAYERS;
            case 6:
                return PROCESSIMAGE;
            case 7:
                return CONTINUEPROCESSIMAGE;
            case 8:
                return STOPPROCESSIMAGE;
            case 9:
                return REMOVEPLAYER;
        }
        return null;
    }
}
