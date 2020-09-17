package com.example.ssocenter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SonSystemInfo {
    private String sonHostAddress;
    private String sonExitUrl;
    private String sessionId;
    private String username;
    private String password;
}
