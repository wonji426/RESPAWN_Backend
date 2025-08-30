package com.shop.respawn.dto;

import java.util.List;

public record OffsetResponse<T>(List<T> items, int offset, int limit, long total) {}