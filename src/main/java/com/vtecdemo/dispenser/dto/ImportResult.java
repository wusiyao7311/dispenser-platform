package com.vtecdemo.dispenser.dto;

import java.util.List;

public record ImportResult(int imported, int skipped, List<String> errors) {
}
