package com.indusnet.service.impl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.indusnet.exception.OtpException;
import com.indusnet.model.OtpData;
import com.indusnet.model.ResendRequest;
import com.indusnet.model.SendOtpRequest;
import com.indusnet.model.ValidateRequest;
import com.indusnet.model.common.OtpStorage;
import com.indusnet.model.common.Type;
import com.indusnet.model.common.ValidationResponce;
import com.indusnet.repository.IOtpRepository;
import com.indusnet.service.IOtpService;
import com.indusnet.util.OtpUtil;
import com.indusnet.util.Util;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * This is Implementation class of IOtpService and override all method.
 * and its also Service layer of project.
 *This class have many service logics.
 */

@Service
@Slf4j
public class OtpServiceImpl implements IOtpService {
	@Autowired
	OtpUtil otpUtil;
	@Autowired
	private IOtpRepository userRepository;
	
	
	OtpStorage otpstr = new OtpStorage();
	OtpData otpData = new OtpData();
	List<OtpStorage> otpList = new ArrayList<>();

	public OtpServiceImpl(OtpUtil otpUtil, IOtpRepository userRepository, OtpStorage otpstr, OtpData otpData,
			List<OtpStorage> otpList) {
		super();
		this.otpUtil = otpUtil;
		this.userRepository = userRepository;
		this.otpstr = otpstr;
		this.otpData = otpData;
		this.otpList = otpList;
	}
	/**
	 * this method is generate the otp and 
	 * return otp.
	 */
	@Override
	public OtpData generateOtp(SendOtpRequest user) throws OtpException {
		if(user.getType().equalsIgnoreCase("sms") && user.getTypeValue().contains("@")) {
			throw new OtpException();
		}
		if(user.getType().equalsIgnoreCase("email") && user.getTypeValue().length() == 10) {
			throw new OtpException();
		}
		
		Optional<OtpData> existedUser = userRepository.findByTypeValue(user.getTypeValue());
		existedUser.ifPresentOrElse(x -> {
			//Secret key
			String secKey = user.getRequeston().toString() +otpUtil.randomValue();
			log.info("secKey "+secKey);
			// OTP generate 
			String otp = Util.generateTOTP256(secKey,120 , "6");
			log.info("OTP is :"+otp);
			String totp =otpUtil.base64encode(Integer.parseInt(otp));
			Integer messageId = otpUtil.randomValue();
			otpstr =otpUtil.checkOtpGenerateCount(otpList, user.getTypeValue());
			if(otpstr != null && otpstr.getCount() > 5) {
				CompletableFuture.delayedExecutor(300, TimeUnit.SECONDS).execute(() -> {
					OtpStorage otpstr1 = otpstr;
					otpList.remove(otpstr);
					otpstr1.setCount(0);
					otpList.add(otpstr1);
				});
				throw new OtpException("you exceed the maximum number of attempt.");
			}

			if(otpstr == null) {
				otpstr = new OtpStorage(null, null, null, 0, null, false);
			}
			OtpStorage otpStorage = new OtpStorage(totp, LocalDateTime.now(),messageId,otpstr.getCount()+1,user.getTypeValue(),false);
			log.info("OTP storage is :"+otpStorage);
			otpList.remove(otpstr);
			otpList.add(otpStorage);
			CompletableFuture.delayedExecutor(600, TimeUnit.SECONDS).execute(() -> otpList.remove(otpStorage));
			log.info("otpId is "+messageId);
			OtpData newUserModel = OtpData.builder()
					.id(x.getId())
					.messageId(messageId)
					.requestdevice(x.getRequestdevice())
					.type(Type.valueOf(user.getType().toUpperCase()))
					.typeValue(x.getTypeValue())
					.user(user.getUser())
					.validupto(LocalDateTime.now().plusMinutes(10))
					.requestedAt(LocalDateTime.now())
					.otpGeneratedAt(LocalDateTime.now())
					.build();
			otpData=userRepository.save(newUserModel);
		}
		,() -> {
			//Secret key
			String secKey = user.getRequeston().toString()+ otpUtil.randomValue();
			// OTP generate 
			String otp = Util.generateTOTP256(secKey,120 , "6");
			log.info("OTP is "+otp);
			String totp =otpUtil.base64encode(Integer.parseInt(otp));	
			Integer messageId = otpUtil.randomValue();
			OtpStorage otpStorage = new OtpStorage(totp, LocalDateTime.now(),messageId,1,user.getTypeValue(),false);
			log.info("OTP storage is :"+otpStorage);
			otpList.add(otpStorage);
			CompletableFuture.delayedExecutor(600, TimeUnit.SECONDS).execute(() -> otpList.remove(otpStorage));
			log.info("otpId is "+messageId);
			OtpData newUserModel = OtpData.builder()
					.requestdevice(user.getRequestdevice())
					.type(Type.valueOf(user.getType().toUpperCase()))
					.messageId(messageId)
					.typeValue(user.getTypeValue())
					.user(user.getUser())
					.validupto(LocalDateTime.now().plusMinutes(10))
					.otpGeneratedAt(LocalDateTime.now())
					.requestedAt(LocalDateTime.now())
					.build();
			otpData=userRepository.save(newUserModel); 
		});
		return otpData;
	}
	/**
	 * This method validate otp and 
	 * return success message if validate successfully or
	 * throw OtpException
	 */
	@Override
	public ValidationResponce validate(ValidateRequest valiRequest) throws OtpException {
		OtpData otpDataDetails=userRepository.getOtpDataByMessageId(valiRequest.getOtpId());
		String otp =otpUtil.base64encode(valiRequest.getOtp());
		if(userRepository.findByMessageId(valiRequest.getOtpId()).isEmpty()) {
			throw new OtpException("");
		}		
		OtpStorage otpStorage = otpUtil.checkOtp(otpList, otp);
		log.info("OTP storage :"+otpStorage);
		CompletableFuture.delayedExecutor(60, TimeUnit.SECONDS).execute(()-> otpList.remove(otpStorage));
		if(otpStorage != null && !otpStorage.isValidate() && LocalDateTime.now().isBefore(otpDataDetails.getValidupto()))	
		{	
			otpStorage.setValidate(true);
			otpList.add(otpList.indexOf(otpStorage),otpStorage);
			otpDataDetails.setValidatedAt(LocalDateTime.now());
			userRepository.save(otpDataDetails);
			return ValidationResponce.builder()
					.status(HttpStatus.OK.value())
					.message("OTP validated successfully")
					.build();
		}
		else if(otpStorage != null && otpStorage.isValidate()) 
		{
			return ValidationResponce.builder()
					.status(HttpStatus.ACCEPTED.value())
					.message("OTP is already being used for validation")
					.build();
		}
		else if(otpDataDetails.getValidupto().isBefore(LocalDateTime.now())) {
			return ValidationResponce.builder()
					.status(HttpStatus.ACCEPTED.value())
					.message("OTP is expired")
					.build();
		}
		else if(otpStorage == null) {
			return ValidationResponce.builder()
					.status(HttpStatus.BAD_REQUEST.value())
					.message("The Submitted OTP is Wrong")
					.build();
		}
		else {
			throw new OtpException();
		}
	}

	/**
	 * This method resend the otp
	 * if user resend otp 5 times 
	 * and after again resend otp then throws OtpException 
	 */
	@Override
	public OtpData resend(ResendRequest resendRequest) throws OtpException {
		if(resendRequest.getType().equals("sms") && resendRequest.getTypeValue().contains("@gmail.com")) {
			throw new OtpException();
		}
		if(resendRequest.getType().equals("email") && resendRequest.getTypeValue().length() == 10) {
			throw new OtpException();
		}
		
		Optional<OtpData> existedOtpData = userRepository.findByMessageId(resendRequest.getLastOtpId());
		existedOtpData.ifPresentOrElse(x -> {
			//Secret key
			String secKey = resendRequest.getRequeston().toString() + otpUtil.randomValue();
			// OTP generate 
			String otp = Util.generateTOTP256(secKey,120 , "6");
			log.info("OTP is "+otp);
			String totp =otpUtil.base64encode(Integer.parseInt(otp));
			int otpId = otpUtil.randomValue();
			otpstr =otpUtil.checkOtpGenerateCount(otpList, resendRequest.getTypeValue());
			if(otpstr != null && otpstr.getCount() > 4) {
				CompletableFuture.delayedExecutor(300, TimeUnit.SECONDS).execute(() -> {
					OtpStorage otpstr1 = otpstr;
					otpList.remove(otpstr);
					otpstr1.setCount(0);
					otpList.add(otpstr1);
				});
				throw new OtpException("your service is block for 2mins and resend after 5mins");
			}

			if(otpstr == null) {
				otpstr = new OtpStorage(null, null, null, 0, null, false);
			}
			OtpStorage otpStorage = new OtpStorage(totp, LocalDateTime.now(),otpId,otpstr.getCount()+1,resendRequest.getTypeValue(),false);
			log.info("OTP storage :"+otpStorage);
			otpList.remove(otpstr);
			otpList.add(otpStorage);
			CompletableFuture.delayedExecutor(600, TimeUnit.SECONDS).execute(() -> otpList.remove(otpStorage));		
			log.info("OTP id is "+otpId);
			OtpData newUserModel = OtpData.builder()
					.id(x.getId())
					.requestdevice(resendRequest.getRequestdevice())
					.type(Type.valueOf(resendRequest.getType().toUpperCase()))
					.typeValue(x.getTypeValue())
					.user(resendRequest.getUser())
					.messageId(otpId)
					.validupto(LocalDateTime.now().plusMinutes(10))
					.requestedAt(LocalDateTime.now())
					.resendInitiatedAt(LocalDateTime.now())
					.otpGeneratedAt(LocalDateTime.now())
					.build();
			otpData=userRepository.save(newUserModel);
		}, () -> {
			throw new OtpException();
		});
		return otpData;
	}
}
