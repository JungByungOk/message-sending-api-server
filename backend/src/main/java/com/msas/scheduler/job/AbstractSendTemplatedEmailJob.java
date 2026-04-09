package com.msas.scheduler.job;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.msas.common.utils.LocalDateTimeDeserializer;
import com.msas.common.utils.LocalDateTimeSerializer;
import com.msas.scheduler.dto.RequestTemplatedEmailScheduleJobDTO;
import com.msas.scheduler.repository.BatchRepository;
import com.msas.ses.dto.MessageTagDto;
import com.msas.ses.dto.RequestTemplatedEmailDto;
import com.msas.ses.repository.EmailResultRepository;
import com.msas.ses.repository.TemplateTenantRepository;
import com.msas.settings.service.ApiGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractSendTemplatedEmailJob extends QuartzJobBean implements InterruptableJob {

    protected ApiGatewayClient apiGatewayClient;
    protected BatchRepository batchRepository;
    protected EmailResultRepository emailResultRepository;
    protected TemplateTenantRepository templateTenantRepository;

    @Autowired
    public void setApiGatewayClient(ApiGatewayClient apiGatewayClient) {
        this.apiGatewayClient = apiGatewayClient;
    }

    @Autowired
    public void setBatchRepository(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Autowired
    public void setEmailResultRepository(EmailResultRepository emailResultRepository) {
        this.emailResultRepository = emailResultRepository;
    }

    @Autowired
    public void setTemplateTenantRepository(TemplateTenantRepository templateTenantRepository) {
        this.templateTenantRepository = templateTenantRepository;
    }

    protected volatile boolean isJobInterrupted = false;

    /**
     * 배치 테이블에서 DTO 로드.
     * batchId로 배치 메타 + 항목을 조회하여 기존 DTO 형태로 변환한다.
     * 기존 형식(JSON 직렬화) Job이 남아있는 경우 폴백 처리한다.
     */
    protected RequestTemplatedEmailScheduleJobDTO deserializeJobData(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String batchId = (String) jobDataMap.get("batchId");

        // 기존 형식 하위 호환: batchId가 없으면 이전 JSON 방식으로 폴백
        if (batchId == null) {
            String strJson = (String) jobDataMap.get("TemplatedEmailScheduleJob");
            if (strJson != null) {
                log.warn("[{}] 기존 형식 Job 감지. JSON 역직렬화로 폴백합니다.", getClass().getSimpleName());
                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
                gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
                return gsonBuilder.create().fromJson(strJson, RequestTemplatedEmailScheduleJobDTO.class);
            }
            throw new JobExecutionException("batchId와 TemplatedEmailScheduleJob 모두 JobDataMap에 없습니다.");
        }

        Gson gson = new Gson();
        Map<String, Object> batch = batchRepository.selectBatch(batchId);
        if (batch == null) {
            throw new JobExecutionException("배치 데이터를 찾을 수 없습니다. (batchId: " + batchId + ")");
        }

        List<Map<String, Object>> items = batchRepository.selectBatchItems(batchId);

        RequestTemplatedEmailScheduleJobDTO dto = new RequestTemplatedEmailScheduleJobDTO();
        dto.setJobName((String) batch.get("job_name"));
        dto.setJobGroup((String) batch.get("job_group"));
        dto.setDescription((String) batch.get("description"));
        dto.setTemplateName((String) batch.get("template_name"));
        dto.setFrom((String) batch.get("from_addr"));
        dto.setTenantId((String) batch.get("tenant_id"));

        // 타입 안전 캐스팅 (드라이버에 따라 Timestamp 또는 LocalDateTime 반환)
        Object startDateAt = batch.get("start_date_at");
        if (startDateAt instanceof java.sql.Timestamp ts) {
            dto.setStartDateAt(ts.toLocalDateTime());
        } else if (startDateAt instanceof LocalDateTime ldt) {
            dto.setStartDateAt(ldt);
        }

        // tags 복원
        String tagsJson = (String) batch.get("tags");
        if (tagsJson != null) {
            dto.setTags(gson.fromJson(tagsJson, new TypeToken<List<MessageTagDto>>() {}.getType()));
        }

        // 이메일 항목 복원
        List<RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto> emailList = new ArrayList<>();
        for (Map<String, Object> item : items) {
            RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto emailDto = new RequestTemplatedEmailScheduleJobDTO.TemplatedEmailDto();
            emailDto.setCorrelationId((String) item.get("correlation_id"));
            Object dtlSeq = item.get("email_send_dtl_seq");
            if (dtlSeq != null) {
                emailDto.setId(String.valueOf(dtlSeq));
            }
            emailDto.setTo(gson.fromJson((String) item.get("to_addrs"), new TypeToken<List<String>>() {}.getType()));
            String ccJson = (String) item.get("cc_addrs");
            if (ccJson != null) {
                emailDto.setCc(gson.fromJson(ccJson, new TypeToken<List<String>>() {}.getType()));
            }
            String bccJson = (String) item.get("bcc_addrs");
            if (bccJson != null) {
                emailDto.setBcc(gson.fromJson(bccJson, new TypeToken<List<String>>() {}.getType()));
            }
            String paramsJson = (String) item.get("template_parameters");
            if (paramsJson != null) {
                emailDto.setTemplateParameters(gson.fromJson(paramsJson, new TypeToken<Map<String, String>>() {}.getType()));
            }
            emailList.add(emailDto);
        }
        dto.setTemplatedEmailList(emailList);

        // 배치 상태를 PROCESSING으로 변경
        batchRepository.updateBatchStatus(batchId, "PROCESSING");

        return dto;
    }

    /**
     * Job 완료 후 배치 데이터 정리
     */
    protected void completeBatch(JobExecutionContext context, String status) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String batchId = (String) jobDataMap.get("batchId");
        if (batchId != null) {
            batchRepository.updateBatchStatus(batchId, status);
            log.info("[{}] 배치 상태 변경. (batchId: {}, status: {})", getClass().getSimpleName(), batchId, status);
        }
    }

    /**
     * API Gateway를 통해 이메일 발송
     */
    protected String sendTemplatedEmail(RequestTemplatedEmailDto dto, String correlationId, String tenantId) {
        try {
            Gson gson = new Gson();
            // correlationId가 없으면 자동 생성
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = java.util.UUID.randomUUID().toString();
            }
            // correlationId와 correlation_id tag를 payload에 주입
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            if (tenantId != null) payload.put("tenantId", tenantId);
            payload.put("correlationId", correlationId);
            payload.put("from", dto.getFrom());
            payload.put("to", dto.getTo());
            payload.put("templateName", dto.getTemplateName());
            payload.put("templateData", dto.getTemplateData());
            if (dto.getCc() != null) payload.put("cc", dto.getCc());
            if (dto.getBcc() != null) payload.put("bcc", dto.getBcc());
            // correlation_id tag 추가
            java.util.List<java.util.Map<String, String>> tags = new java.util.ArrayList<>();
            tags.add(java.util.Map.of("name", "correlation_id", "value", correlationId));
            if (dto.getTags() != null) {
                dto.getTags().forEach(t -> tags.add(java.util.Map.of("name", t.getName(), "value", t.getValue())));
            }
            payload.put("tags", tags);
            String jsonBody = gson.toJson(payload);
            var response = apiGatewayClient.post("/send-email", jsonBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                java.util.Map<String, String> resultMap = gson.fromJson(response.body(),
                        new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>() {}.getType());
                return resultMap.getOrDefault("messageId", "");
            } else {
                log.warn("[EmailJob] 발송 실패. (HTTP {}, template: {})", response.statusCode(), dto.getTemplateName());
                throw new RuntimeException("이메일 발송 실패 (HTTP " + response.statusCode() + ")");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmailJob] 발송 실패. (template: {})", dto.getTemplateName(), e);
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 반복 스케줄 시 이전 interrupt된 Job을 스케줄에서 제거
     */
    protected void deleteJobIfNotInterrupted(JobExecutionContext context) throws JobExecutionException {
        if (!isJobInterrupted) {
            try {
                context.getScheduler().deleteJob(context.getJobDetail().getKey());
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
        }
    }

    /**
     * 대량 발송 이메일 그룹에서 개별 이메일 정보 가져오기
     */
    protected RequestTemplatedEmailDto getTemplatedEmailDto(RequestTemplatedEmailScheduleJobDTO scheduleData, int idx) {
        RequestTemplatedEmailDto dto = new RequestTemplatedEmailDto();
        dto.setFrom(scheduleData.getFrom());
        dto.setTo(scheduleData.getTemplatedEmailList().get(idx).getTo());
        dto.setTemplateName(scheduleData.getTemplateName());
        dto.setTemplateData(scheduleData.getTemplatedEmailList().get(idx).getTemplateParameters());
        dto.setCc(scheduleData.getTemplatedEmailList().get(idx).getCc());
        dto.setBcc(scheduleData.getTemplatedEmailList().get(idx).getBcc());
        dto.setTags(scheduleData.getTags());
        return dto;
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        isJobInterrupted = true;
        log.info("[{}] 작업 인터럽트 요청 수신.", getClass().getSimpleName());
    }
}
