package com.murilloskills.network;
/** Transient server-authoritative operation feedback, never persisted as progression. */
public record ActivityView(String kind,String phase,String title,String detail,int done,int total,int affected,long until) {}
