package com.jitlogic.zorka.common.cbor;

public class TextIndexTypeMarkers {

    public static final byte STRING_TYPE  = 0x00; // Generic string, raw encoding (no prefix);

    public static final byte TYPE_MIN     = 0x04;
    public static final byte KEYWORD_TYPE = 0x04; // LISP keyword:     0x04|keyword_name|0x04
    public static final byte CLASS_TYPE   = 0x05; // Class name        0x05|class_name|0x05
    public static final byte METHOD_TYPE  = 0x06; // Method name       0x06|method_name|0x06
    public static final byte UUID_TYPE    = 0x07; // UUID              0x07|uuid_encoded|0x07
    public static final byte SIGN_TYPE    = 0x08; // Method signature  0x08|method_signature|0x08
    public static final byte TYPE_MAX     = 0x08;

}
