package com.murilloskills.validation;
import java.util.*;
public final class Checks {
    public static final List<String> results=new ArrayList<>();
    private Checks(){}
    public static void check(boolean value,String description){if(!value)throw new AssertionError(description);results.add("PASS "+description);}
}

