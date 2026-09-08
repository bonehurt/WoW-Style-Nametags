package com.wownpc;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NpcMaxHitPosition {
	AFTER("After"),
	BEFORE("Before");

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
