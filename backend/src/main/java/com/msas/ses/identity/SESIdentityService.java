package com.msas.ses.identity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(SesV2Client.class)
public class SESIdentityService {

    private final SesV2Client sesV2Client;

    /**
     * 도메인 이메일 아이덴티티를 생성하고 DKIM 레코드를 반환합니다.
     */
    public DkimRecordsDTO createDomainIdentity(String domain) {
        CreateEmailIdentityRequest request = CreateEmailIdentityRequest.builder()
                .emailIdentity(domain)
                .build();

        CreateEmailIdentityResponse response = sesV2Client.createEmailIdentity(request);
        log.info("SESIdentityService - 도메인 아이덴티티 생성 완료. (domain: {})", domain);

        String verificationStatus = response.verifiedForSendingStatus() != null && response.verifiedForSendingStatus()
                ? "SUCCESS" : "PENDING";

        List<DkimRecordDTO> records = List.of();
        if (response.dkimAttributes() != null && response.dkimAttributes().tokens() != null) {
            records = response.dkimAttributes().tokens().stream()
                    .map(token -> new DkimRecordDTO(
                            token + "._domainkey." + domain,
                            "CNAME",
                            token + ".dkim.amazonses.com"))
                    .collect(Collectors.toList());
        }

        return new DkimRecordsDTO(domain, verificationStatus, records);
    }

    /**
     * 도메인 이메일 아이덴티티의 인증 상태를 조회합니다.
     */
    public DkimRecordsDTO getDomainVerificationStatus(String domain) {
        GetEmailIdentityRequest request = GetEmailIdentityRequest.builder()
                .emailIdentity(domain)
                .build();

        GetEmailIdentityResponse response = sesV2Client.getEmailIdentity(request);
        log.info("SESIdentityService - 도메인 인증 상태 조회. (domain: {})", domain);

        String verificationStatus = response.verifiedForSendingStatus() != null && response.verifiedForSendingStatus()
                ? "SUCCESS" : "PENDING";

        List<DkimRecordDTO> records = List.of();
        if (response.dkimAttributes() != null && response.dkimAttributes().tokens() != null) {
            records = response.dkimAttributes().tokens().stream()
                    .map(token -> new DkimRecordDTO(
                            token + "._domainkey." + domain,
                            "CNAME",
                            token + ".dkim.amazonses.com"))
                    .collect(Collectors.toList());
        }

        return new DkimRecordsDTO(domain, verificationStatus, records);
    }

    /**
     * 도메인 이메일 아이덴티티를 삭제합니다.
     */
    public void deleteDomainIdentity(String domain) {
        DeleteEmailIdentityRequest request = DeleteEmailIdentityRequest.builder()
                .emailIdentity(domain)
                .build();

        sesV2Client.deleteEmailIdentity(request);
        log.info("SESIdentityService - 도메인 아이덴티티 삭제 완료. (domain: {})", domain);
    }
}
