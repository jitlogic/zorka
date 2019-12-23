package com.jitlogic.zorka.common.cbor;

public class TextIndexTypeMarkers {

    public static final byte STRING_TYPE  = 0x00; // Generic string, raw encoding (no prefix);

    public static final byte CLASS_TYPE   = 0x05; // Class name        0x05|class_name|0x05
    public static final byte METHOD_TYPE  = 0x06; // Method name       0x06|method_name|0x06
    public static final byte SIGN_TYPE    = 0x08; // Method signature  0x08|method_signature|0x08

}
