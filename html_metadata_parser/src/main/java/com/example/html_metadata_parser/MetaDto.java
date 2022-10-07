package com.example.html_metadata_parser;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetaDto {
    private String title;
    private String url;
    private String description;
    private String cover;
}
