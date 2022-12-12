package com.indusnet.util;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;
import org.springframework.stereotype.Component;
import com.indusnet.model.common.OtpStorage;
@Component
public class OtpUtil {
	public String base64encode(Integer value) {
		byte[] mId = DatatypeConverter.parseHexBinary(value.toString());
		return Base64.getEncoder().encodeToString(mId);
	}
	public int randomValue() {
		Integer messageId = 100000 + Math.abs(new Random().nextInt() * 899900);
		while(messageId > 999999) {
			messageId = messageId / 10;
		}
		return messageId;
	}
	
	public OtpStorage checkOtp(List<OtpStorage> otpList,String otp) {
		for(OtpStorage o: otpList){
			if(otp.equals(o.getOtp())) {
				return o;
			}
		}
		return null;
	}
	
	public OtpStorage checkOtpGenerateCount(List<OtpStorage> otpList,String type) {
		for(OtpStorage o: otpList){
			if(type.equals(o.getType())) {
				return o;
			}
		}
		
		return null;
	}
}
