package com.jitlogic.zorka.common.codec;

public class StructuredTextIndex {
    // TODO implement proper escaping here

    public static final byte STRING_TYPE  = 0x00; // Generic string, raw encoding (no prefix);

    public static final byte TYPE_MIN     = 0x04;
    public static final byte KEYWORD_TYPE = 0x04; // LISP keyword:     0x04|keyword_name|0x04
    public static final byte CLASS_TYPE   = 0x05; // Class name        0x05|class_name|0x05
    public static final byte METHOD_TYPE  = 0x06; // Method name       0x06|method_name|0x06
    public static final byte UUID_TYPE    = 0x07; // UUID              0x07|uuid_encoded|0x07
    public static final byte SIGN_TYPE    = 0x08; // Method signature  0x08|method_signature|0x08
    public static final byte TYPE_MAX     = 0x08;

    public static final byte KV_PAIR      = 0x09;
    public static final byte KR_PAIR      = 0x0a;

    public static final byte METHOD_DESC     = 0x0b;  // Method description   0x0b|cid|0x0b|mid|0x0b|sid|0x0b
    public static final byte EXCEPTION_DESC  = 0x0c;
    public static final byte CALL_STACK_DESC = 0x0d;
    public static final byte STACK_ITEM_DESC = 0x0e;
    public static final byte AGENT_ATTR_DESC = 0x0f;
}
