package com.shop.respawn.dto;

import java.util.List;

public record OffsetPage<T>(List<T> items, long total) {}