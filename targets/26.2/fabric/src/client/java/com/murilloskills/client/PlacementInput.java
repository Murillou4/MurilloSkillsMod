package com.murilloskills.client;
import com.murilloskills.network.Protocol;
public final class PlacementInput {
    private static boolean armed;
    private PlacementInput(){}
    public static boolean armed(){return armed;}
    public static void reset(){armed=false;}
    public static void press(){if(ClientPreferences.current.placementContinuous){armed=!armed;ClientState.message="UltPlace "+(armed?"armado: clique direito para construir":"desarmado");ClientState.messageUntil=ClientState.clientTicks+80;}else ClientState.send(Protocol.Op.PLACE,"",0);}
}
