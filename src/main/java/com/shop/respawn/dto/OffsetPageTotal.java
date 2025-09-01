package com.shop.respawn.dto;

import java.util.List;

public record OffsetPageTotal<T>(List<T> items, long total, long writtenTotal) {}
