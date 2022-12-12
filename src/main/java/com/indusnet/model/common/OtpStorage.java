package com.indusnet.model.common;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Component
public class OtpStorage {

	private String otp;
	private LocalDateTime validUpto;
	private Integer otpId;
	private Integer count;
	private String type;
	private boolean isValidate;
}
