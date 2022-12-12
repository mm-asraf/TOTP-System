package com.indusnet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.indusnet.model.OtpData;
import com.indusnet.model.SendOtpRequest;
import com.indusnet.model.UserModel;
import com.indusnet.model.common.OtpStorage;
import com.indusnet.repository.IOtpRepository;
import com.indusnet.service.IOtpService;
import com.indusnet.service.impl.OtpServiceImpl;
import com.indusnet.util.OtpUtil;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
class TotpApplicationTests {

	@Autowired
	IOtpService iUserService;

	@MockBean
	IOtpRepository iUserRepository;

	@Autowired
	OtpUtil otpUtil;
	OtpStorage otpstr = new OtpStorage();
	OtpData otpData = new OtpData();
	List<OtpStorage> otpList = new ArrayList<>();

	@BeforeEach
	void setUp(){
		this.iUserService = new OtpServiceImpl(otpUtil,iUserRepository,otpstr,otpData,otpList);
	}

	/**
	 * This Test case for otp generation 
	 * if otp generate successfully  and otp is 6 digit then test case is pass.
	 */

	@Test
	@Order(1)
	void otpSendTest() {
		SendOtpRequest otpModel = SendOtpRequest.builder()
				.typeValue("arif841236@gmail.com")
				.type(com.indusnet.model.common.Type.EMAIL.name())
				.requestdevice("2C-23-45-14-23-AD")
				.requeston((int)Instant.now().getEpochSecond())
				.user(new UserModel(null, 1))
				.build();
		iUserService.generateOtp(otpModel);
		when(otpList.get(0).getOtp().length() == 6).thenReturn(true);
		System.out.println(otpList);
		assertEquals(4, otpList.get(0).getOtp().length());
	}


	//	@Test
	//	@Order(2)
	//	void otpValidateTest() {
	//
	//		SendOtpRequest otpModel = SendOtpRequest.builder()
	//				.typeValue("9804560079")
	//				.type(Type.SMS.name())
	//				.requestdevice("2C-23-45-14-23-AD")
	//				.requeston((int)Instant.now().getEpochSecond())
	//				.user(new UserModel(null, 1))
	//				.build();
	//		iUserService.generateOtp(otpModel);
	//		ValidateRequest validRequest = new ValidateRequest(Integer.parseInt(otpList.get(0).getOtp()), otpList.get(0).getOtpId(), (int)Instant.now().getEpochSecond(), "2C-23-45-14-23-AD");
	//		ValidationResponce responce = iUserService.validate(validRequest);
	//		System.out.println(otpList);
	//		assertEquals(HttpStatus.OK, responce.getStatus());
	//		
	//	}

}

