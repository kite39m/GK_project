package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class ModuleStatDTO {
    private String module;
    private String moduleName;
    private Integer total;
    private Integer correct;
    private Double accuracy;
}
