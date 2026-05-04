package com.zwy.gk_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisReport {
    private String rawReport;
    private List<WeakModule> weakModules;

    @Data
    public static class WeakModule {
        private String module;
        private String moduleName;
        private Double accuracy;
        private String errorCategory;
        private String suggestion;
    }
}
